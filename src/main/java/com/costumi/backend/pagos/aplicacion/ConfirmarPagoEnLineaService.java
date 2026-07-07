package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.IntentoDePago;
import com.costumi.backend.pagos.dominio.IntentoDePagoRepository;
import com.costumi.backend.pagos.dominio.MetodoPago;
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

	ConfirmarPagoEnLineaService(IntentoDePagoRepository intentos, RegistrarPago registrarPago) {
		this.intentos = intentos;
		this.registrarPago = registrarPago;
	}

	@Override
	@Transactional
	public void ejecutar(UUID intentoId, String idPagoExterno) {
		IntentoDePago intento = intentos.buscarPorId(intentoId).orElse(null);
		if (intento == null || intento.estaConfirmado()) {
			return; // desconocido o ya confirmado: idempotente
		}
		String clave = "pasarela:" + idPagoExterno;
		registrarPago.ejecutar(new RegistrarPagoComando(intento.empresaId(), intento.sucursalId(),
				intento.empleadoId(), intento.tipoConcepto(), intento.conceptoId(), intento.monto(),
				TipoPago.COBRO, MetodoPago.TARJETA, clave, clave));
		intento.confirmar();
		intentos.guardar(intento);
	}
}
