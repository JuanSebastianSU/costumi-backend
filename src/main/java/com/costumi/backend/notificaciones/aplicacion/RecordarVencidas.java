package com.costumi.backend.notificaciones.aplicacion;

import java.util.UUID;

/** Puerto de entrada: envía a cada cliente un recordatorio de su devolución vencida (RF-11.1). */
public interface RecordarVencidas {

	/** Envía los recordatorios y devuelve cuántos se enviaron. */
	int ejecutar(UUID empresaId);
}
