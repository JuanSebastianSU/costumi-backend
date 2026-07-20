package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.clientes.ResolucionDeClientes;
import com.costumi.backend.configuracion.ConsultaDeConfiguracion;
import com.costumi.backend.pagos.dominio.IntentoDePago;
import com.costumi.backend.pagos.dominio.IntentoDePagoRepository;
import com.costumi.backend.pagos.dominio.PagoRepository;
import com.costumi.backend.pagos.dominio.PasarelaDePago;
import com.costumi.backend.pagos.dominio.TipoConcepto;
import com.costumi.backend.rentas.ConsultaDeRentas;
import com.costumi.backend.ventas.ConsultaDeVentas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Inicia un pago en línea (RF-6.11): exige que la empresa tenga el switch pagoEnLinea activo. Como el
 * pago con tarjeta es <b>todo de golpe</b> (sin adelantos), el monto NO lo decide el cliente: el backend
 * calcula el <b>saldo pendiente</b> del concepto (total − ya pagado) y cobra exactamente eso; si el monto
 * enviado no lo cubre, se rechaza (C-pagos: el monto se valida contra el importe real, no se confía en el cliente).
 */
@Service
class CrearIntentoDePagoService implements CrearIntentoDePago, CrearIntentoDePagoDeCliente {

	private final IntentoDePagoRepository intentos;
	private final PasarelaDePago pasarela;
	private final ConsultaDeConfiguracion configuracion;
	private final ConsultaDeRentas rentas;
	private final ConsultaDeVentas ventas;
	private final PagoRepository pagos;
	private final ResolucionDeClientes clientes;

	CrearIntentoDePagoService(IntentoDePagoRepository intentos, PasarelaDePago pasarela,
			ConsultaDeConfiguracion configuracion, ConsultaDeRentas rentas, ConsultaDeVentas ventas,
			PagoRepository pagos, ResolucionDeClientes clientes) {
		this.intentos = intentos;
		this.pasarela = pasarela;
		this.configuracion = configuracion;
		this.rentas = rentas;
		this.ventas = ventas;
		this.pagos = pagos;
		this.clientes = clientes;
	}

	@Override
	@Transactional
	public Resultado ejecutar(UUID empresaId, UUID sucursalId, UUID empleadoId, TipoConcepto tipoConcepto,
			UUID conceptoId, BigDecimal monto, String moneda) {
		if (!configuracion.pagoEnLinea(empresaId)) {
			throw new PagoEnLineaDeshabilitado();
		}
		BigDecimal total = totalDelConcepto(empresaId, tipoConcepto, conceptoId);
		BigDecimal pendiente = total.subtract(pagos.saldoNetoPorConcepto(empresaId, conceptoId));
		if (pendiente.signum() <= 0) {
			throw new IllegalArgumentException("La operación no tiene saldo pendiente por cobrar");
		}
		// Con tarjeta se paga TODO de golpe: el monto debe cubrir el total pendiente exacto (ni parcial ni de más).
		if (monto == null || monto.compareTo(pendiente) != 0) {
			throw new IllegalArgumentException("El pago en línea debe cubrir el total pendiente (" + pendiente + ")");
		}
		IntentoDePago intento = IntentoDePago.crear(empresaId, sucursalId, empleadoId, tipoConcepto, conceptoId,
				pendiente, moneda);
		PasarelaDePago.ResultadoCheckout checkout = pasarela.crearCheckout(intento.monto(), intento.moneda(),
				intento.id().toString(), "Pago " + tipoConcepto + " " + conceptoId);
		intento.asignarReferenciaExterna(checkout.idExterno());
		intentos.guardar(intento);
		return new Resultado(intento.id(), checkout.urlCheckout());
	}

	/**
	 * Intento de pago iniciado por el propio cliente del marketplace (RF-14.4): resuelve su ficha en la tienda
	 * y verifica que la operación sea suya antes de crear el checkout (no revela pedidos ajenos, 403). El resto
	 * (validación de monto contra el total pendiente, switch de pago en línea) es idéntico al del personal.
	 */
	@Override
	@Transactional
	public Resultado ejecutar(CrearIntentoDePagoDeClienteComando comando) {
		UUID ficha = clientes.fichaDeUsuarioSiExiste(comando.empresaId(), comando.usuarioId())
				.orElseThrow(PagoEnLineaNoAutorizado::new);
		UUID clienteDelConcepto = switch (comando.tipoConcepto()) {
			case VENTA -> ventas.clienteDeVenta(comando.empresaId(), comando.conceptoId()).orElse(null);
			case RENTA -> rentas.clienteDeRenta(comando.empresaId(), comando.conceptoId()).orElse(null);
		};
		if (clienteDelConcepto == null || !clienteDelConcepto.equals(ficha)) {
			throw new PagoEnLineaNoAutorizado();
		}
		// El cliente que inicia el pago se atribuye como quien lo confirma (no hay staff en el flujo del marketplace).
		return ejecutar(comando.empresaId(), comando.sucursalId(), comando.usuarioId(), comando.tipoConcepto(),
				comando.conceptoId(), comando.monto(), comando.moneda());
	}

	/** Monto total a cobrar del concepto (importe de la renta o total de la venta); falla si no existe. */
	private BigDecimal totalDelConcepto(UUID empresaId, TipoConcepto tipoConcepto, UUID conceptoId) {
		return switch (tipoConcepto) {
			case RENTA -> rentas.importeDeRenta(empresaId, conceptoId)
					.orElseThrow(() -> new IllegalArgumentException("La renta a cobrar no existe en esta empresa"));
			case VENTA -> ventas.totalDeVenta(empresaId, conceptoId)
					.orElseThrow(() -> new IllegalArgumentException("La venta a cobrar no existe en esta empresa"));
		};
	}
}
