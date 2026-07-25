package com.costumi.backend.notificaciones.adaptadores.entrada;

import com.costumi.backend.notificaciones.dominio.Notificacion;

import java.time.Instant;
import java.util.UUID;

/** DTO de salida de la Notificación. */
public record NotificacionResponse(UUID id, UUID clienteId, String clienteNombre, String canal, String mensaje,
		String estado, Instant fecha) {

	/** Con el nombre del cliente ya resuelto (listados: se resuelve por lote para evitar N+1). */
	static NotificacionResponse desde(Notificacion n, String clienteNombre) {
		return new NotificacionResponse(n.id(), n.clienteId(), clienteNombre, n.canal().name(), n.mensaje(),
				n.estado().name(), n.fecha());
	}

	/** Sin nombre resuelto (respuesta de una sola notificación recién enviada). */
	static NotificacionResponse desde(Notificacion n) {
		return desde(n, null);
	}
}
