package com.costumi.backend.auditoria.adaptadores.entrada;

import com.costumi.backend.auditoria.dominio.RegistroDeAuditoria;

import java.time.Instant;
import java.util.UUID;

/** DTO de salida de un registro de auditoría. */
public record AuditoriaResponse(UUID id, String accion, String detalle, Instant fecha) {

	static AuditoriaResponse desde(RegistroDeAuditoria r) {
		return new AuditoriaResponse(r.id(), r.accion(), r.detalle(), r.fecha());
	}
}
