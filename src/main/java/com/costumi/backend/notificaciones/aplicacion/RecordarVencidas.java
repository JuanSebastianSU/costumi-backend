package com.costumi.backend.notificaciones.aplicacion;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: envía a cada cliente un recordatorio de su devolución vencida (RF-11.1). */
public interface RecordarVencidas {

	/** Envía los recordatorios y devuelve cuántos se enviaron. */
	int ejecutar(UUID empresaId);

	/** Empresas con al menos una renta vencida hoy (RF-3.5): las que el job programado debe recordar. */
	List<UUID> empresasConVencidas();
}
