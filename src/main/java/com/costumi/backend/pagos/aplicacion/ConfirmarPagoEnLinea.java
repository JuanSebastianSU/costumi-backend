package com.costumi.backend.pagos.aplicacion;

import java.util.UUID;

/** Puerto de entrada: confirmar un pago en línea desde el webhook de la pasarela (RF-6.11). */
public interface ConfirmarPagoEnLinea {

	/** Registra el Pago del intento (idempotente por el id externo) y marca el intento CONFIRMADO. */
	void ejecutar(UUID intentoId, String idPagoExterno);
}
