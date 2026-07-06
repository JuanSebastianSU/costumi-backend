package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.CobroMixto;
import com.costumi.backend.pagos.dominio.Pago;
import com.costumi.backend.pagos.dominio.PagoRepository;
import com.costumi.backend.pagos.dominio.PorcionDePago;
import com.costumi.backend.configuracion.ConsultaDeConfiguracion;
import com.costumi.backend.pagos.dominio.TipoConcepto;
import com.costumi.backend.pagos.dominio.TipoPago;
import com.costumi.backend.rentas.ConsultaDeRentas;
import com.costumi.backend.ventas.ConsultaDeVentas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Casos de uso de Pagos, acotados a la empresa (tenant), con idempotencia opcional. */
@Service
class PagoService implements RegistrarPago, ConsultarPagos, RegistrarCobroMixto {

	private final PagoRepository pagos;
	private final ConsultaDeRentas rentas;
	private final ConsultaDeVentas ventas;
	private final ConsultaDeConfiguracion configuracion;

	PagoService(PagoRepository pagos, ConsultaDeRentas rentas, ConsultaDeVentas ventas,
			ConsultaDeConfiguracion configuracion) {
		this.pagos = pagos;
		this.rentas = rentas;
		this.ventas = ventas;
		this.configuracion = configuracion;
	}

	@Override
	@Transactional
	public Pago ejecutar(RegistrarPagoComando comando) {
		if (comando.claveIdempotencia() != null && !comando.claveIdempotencia().isBlank()) {
			Optional<Pago> existente = pagos.buscarPorClave(comando.empresaId(), comando.claveIdempotencia().trim());
			if (existente.isPresent()) {
				return existente.get(); // idempotente: no se duplica el cobro
			}
		}
		return pagos.guardar(Pago.registrar(comando.empresaId(), comando.sucursalId(), comando.empleadoId(),
				comando.tipoConcepto(), comando.conceptoId(), comando.monto(), comando.tipoPago(), comando.metodo(),
				comando.referencia(), comando.claveIdempotencia()));
	}

	@Override
	@Transactional
	public ResultadoCobroMixto ejecutar(RegistrarCobroMixtoComando comando) {
		// El dominio valida las porciones y calcula el vuelto (rechaza efectivo insuficiente, RF-6.7).
		CobroMixto calculo = new CobroMixto(comando.porciones(), comando.efectivoRecibido());
		String claveBase = (comando.claveIdempotencia() == null || comando.claveIdempotencia().isBlank()) ? null
				: comando.claveIdempotencia().trim();
		// En un reintento idempotente las porciones ya existen: no se revalida el saldo (ya está cobrado).
		boolean reintento = claveBase != null
				&& pagos.buscarPorClave(comando.empresaId(), claveBase + "#0").isPresent();
		if (!reintento) {
			// El cobro mixto liquida el saldo pendiente: no puede cobrar de más ni de menos (RF-6.1).
			BigDecimal total = totalDelConcepto(comando.empresaId(), comando.tipoConcepto(), comando.conceptoId());
			BigDecimal saldoPendiente = total.subtract(pagos.saldoNetoPorConcepto(comando.empresaId(),
					comando.conceptoId()));
			if (calculo.total().compareTo(saldoPendiente) != 0) {
				throw new IllegalArgumentException("El cobro (" + calculo.total()
						+ ") no cuadra con el saldo pendiente (" + saldoPendiente + ")");
			}
		}
		List<Pago> generados = new ArrayList<>();
		int indice = 0;
		for (PorcionDePago porcion : comando.porciones()) {
			// Cada porción hereda la idempotencia del cobro con un sufijo, para no duplicar al reintentar.
			String clave = claveBase == null ? null : claveBase + "#" + indice;
			generados.add(ejecutar(new RegistrarPagoComando(comando.empresaId(), comando.sucursalId(),
					comando.empleadoId(), comando.tipoConcepto(), comando.conceptoId(), porcion.monto(), TipoPago.COBRO,
					porcion.metodo(), porcion.referencia(), clave)));
			indice++;
		}
		return new ResultadoCobroMixto(generados, calculo.total(), calculo.vuelto());
	}

	/** Monto total a cobrar del concepto (importe de renta o total de venta); falla si no existe en la empresa. */
	private BigDecimal totalDelConcepto(UUID empresaId, TipoConcepto tipoConcepto, UUID conceptoId) {
		Optional<BigDecimal> total = switch (tipoConcepto) {
			case RENTA -> rentas.importeDeRenta(empresaId, conceptoId);
			case VENTA -> ventas.totalDeVenta(empresaId, conceptoId);
		};
		return total.orElseThrow(
				() -> new IllegalArgumentException("La operación a cobrar no existe en esta empresa"));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Pago> deConcepto(UUID empresaId, UUID conceptoId) {
		return pagos.listarPorConcepto(empresaId, conceptoId);
	}

	@Override
	@Transactional(readOnly = true)
	public BigDecimal saldoNeto(UUID empresaId, UUID conceptoId) {
		return pagos.saldoNetoPorConcepto(empresaId, conceptoId);
	}

	@Override
	@Transactional(readOnly = true)
	public EstadoDeposito estadoDeposito(UUID empresaId, UUID conceptoId) {
		return estadoDeposito(conceptoId, pagos.listarPorConcepto(empresaId, conceptoId));
	}

	@Override
	@Transactional(readOnly = true)
	public Comprobante comprobante(UUID empresaId, UUID conceptoId) {
		List<Pago> delConcepto = pagos.listarPorConcepto(empresaId, conceptoId);
		BigDecimal totalCobrado = sumaPorTipo(delConcepto, TipoPago.COBRO);
		BigDecimal totalReembolsado = sumaPorTipo(delConcepto, TipoPago.REEMBOLSO);
		BigDecimal saldoNeto = delConcepto.stream().map(Pago::montoNeto).reduce(BigDecimal.ZERO, BigDecimal::add);
		// Impuesto-incluido (RF-6.5): el total cobrado ya lo trae; se desglosa base + impuesto según la tasa.
		BigDecimal tasa = configuracion.tasaImpuesto(empresaId);
		BigDecimal base = totalCobrado.divide(BigDecimal.ONE.add(tasa), 2, RoundingMode.HALF_UP);
		BigDecimal impuesto = totalCobrado.subtract(base);
		return new Comprobante(conceptoId, delConcepto, totalCobrado, totalReembolsado, saldoNeto,
				estadoDeposito(conceptoId, delConcepto), tasa, base, impuesto);
	}

	private static EstadoDeposito estadoDeposito(UUID conceptoId, List<Pago> delConcepto) {
		BigDecimal retenido = sumaPorTipo(delConcepto, TipoPago.DEPOSITO);
		BigDecimal devuelto = sumaPorTipo(delConcepto, TipoPago.DEVOLUCION_DEPOSITO);
		return new EstadoDeposito(conceptoId, retenido, devuelto, retenido.subtract(devuelto));
	}

	private static BigDecimal sumaPorTipo(List<Pago> pagos, TipoPago tipo) {
		return pagos.stream().filter(p -> p.tipoPago() == tipo).map(Pago::monto)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
