package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.CobroMixto;
import com.costumi.backend.pagos.dominio.Pago;
import com.costumi.backend.pagos.dominio.PagoRepository;
import com.costumi.backend.pagos.dominio.PorcionDePago;
import com.costumi.backend.pagos.dominio.TipoPago;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Casos de uso de Pagos, acotados a la empresa (tenant), con idempotencia opcional. */
@Service
class PagoService implements RegistrarPago, ConsultarPagos, RegistrarCobroMixto {

	private final PagoRepository pagos;

	PagoService(PagoRepository pagos) {
		this.pagos = pagos;
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
		List<Pago> delConcepto = pagos.listarPorConcepto(empresaId, conceptoId);
		BigDecimal retenido = sumaPorTipo(delConcepto, TipoPago.DEPOSITO);
		BigDecimal devuelto = sumaPorTipo(delConcepto, TipoPago.DEVOLUCION_DEPOSITO);
		return new EstadoDeposito(conceptoId, retenido, devuelto, retenido.subtract(devuelto));
	}

	private static BigDecimal sumaPorTipo(List<Pago> pagos, TipoPago tipo) {
		return pagos.stream().filter(p -> p.tipoPago() == tipo).map(Pago::monto)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
