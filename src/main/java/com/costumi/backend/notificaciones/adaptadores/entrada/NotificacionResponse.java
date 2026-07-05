package com.costumi.backend.notificaciones.adaptadores.entrada;

import com.costumi.backend.notificaciones.dominio.Notificacion;

import java.time.Instant;
import java.util.UUID;

/** DTO de salida de la Notificación. */
public record NotificacionResponse(UUID id, UUID clienteId, String canal, String mensaje, String estado,
		Instant fecha) {

	static NotificacionResponse desde(Notificacion n) {
		return new NotificacionResponse(n.id(), n.clienteId(), n.canal().name(), n.mensaje(), n.estado().name(),
				n.fecha());
	}
}
