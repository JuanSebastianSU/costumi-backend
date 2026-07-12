package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.IntentoDePago;
import com.costumi.backend.pagos.dominio.IntentoDePagoRepository;
import com.costumi.backend.pagos.dominio.MetodoPago;
import com.costumi.backend.pagos.dominio.PasarelaDePago;
import com.costumi.backend.pagos.dominio.TipoPago;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Confirma un pago en línea desde el webhook (RF-6.11): registra el {@link com.costumi.backend.pagos.dominio.Pago}
 * del intento (idempotente por el id externo del pago) y marca el intento CONFIRMADO. Si el intento no
 * existe o ya está confirmado, no hace nada (idempotente frente a webhooks repetidos).
 */
@Service
class ConfirmarPagoEnLineaService implements ConfirmarPagoEnLinea {

	private final IntentoDePagoRepository intentos;
	private final RegistrarPago registrarPago;
	private final PasarelaDePago pasarela;

	ConfirmarPagoEnLineaService(IntentoDePagoRepository intentos, RegistrarPago registrarPago,
			PasarelaDePago pasarela) {
		this.intentos = intentos;
		this.registrarPago = registrarPago;
		this.pasarela = pasarela;
	}

	@Override
	@Transactional
	public void ejecutar(UUID intentoId, String idPagoExterno) {
		IntentoDePago intento = intentos.buscarPorId(intentoId).orElse(null);
		if (intento == null || intento.estaConfirmado()) {
			return; // desconocido o ya confirmado: idempotente
		}
		// P-3: si hay credenciales, se verifica contra el proveedor antes de confirmar (no se confía solo en
		// el webhook, aunque venga firmado). Sin credenciales (dev/test) se mantiene el comportamiento previo.
		if (pasarela.configurada() && !verificadoContraProveedor(intento, idPagoExterno)) {
			return; // el proveedor no reporta el pago como aprobado: no se confirma (idempotente)
		}
		String clave = "pasarela:" + idPagoExterno;
		registrarPago.ejecutar(new RegistrarPagoComando(intento.empresaId(), intento.sucursalId(),
				intento.empleadoId(), intento.tipoConcepto(), intento.conceptoId(), intento.monto(),
				TipoPago.COBRO, MetodoPago.TARJETA, clave, clave));
		intento.confirmar();
		intentos.guardar(intento);
	}

	/**
	 * P-3: consulta el pago en el proveedor. {@code true} solo si está aprobado; si el monto informado no
	 * coincide con el del intento es una anomalía y se corta con {@link VerificacionDePagoFallida} (409).
	 */
	private boolean verificadoContraProveedor(IntentoDePago intento, String idPagoExterno) {
		PasarelaDePago.EstadoPagoExterno estado = pasarela.consultarPago(idPagoExterno);
		if (!estado.aprobado()) {
			return false;
		}
		if (estado.monto() != null && estado.monto().compareTo(intento.monto()) != 0) {
			throw new VerificacionDePagoFallida("El monto del pago en el proveedor (" + estado.monto()
					+ ") no coincide con el del intento (" + intento.monto() + ")");
		}
		return true;
	}
}
