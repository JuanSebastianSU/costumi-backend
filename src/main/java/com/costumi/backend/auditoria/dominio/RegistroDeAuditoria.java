package com.costumi.backend.auditoria.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro de auditoría (RF-0.5/15.5): deja constancia de una acción relevante (dinero/inventario o de
 * plataforma) de una empresa. Se alimenta de los domain events (§5.5): quién/qué/cuándo, inmutable.
 */
public class RegistroDeAuditoria {

	private final UUID id;
	private final UUID empresaId;
	private final String accion;
	private final String detalle;
	private final Instant fecha;

	private RegistroDeAuditoria(UUID id, UUID empresaId, String accion, String detalle, Instant fecha) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		if (accion == null || accion.isBlank()) {
			throw new IllegalArgumentException("La acción de auditoría es obligatoria");
		}
		this.accion = accion.trim();
		this.detalle = (detalle == null || detalle.isBlank()) ? null : detalle.trim();
		this.fecha = Objects.requireNonNull(fecha, "fecha");
	}

	public static RegistroDeAuditoria de(UUID empresaId, String accion, String detalle) {
		return new RegistroDeAuditoria(UUID.randomUUID(), empresaId, accion, detalle, Instant.now());
	}

	public static RegistroDeAuditoria rehidratar(UUID id, UUID empresaId, String accion, String detalle, Instant fecha) {
		return new RegistroDeAuditoria(id, empresaId, accion, detalle, fecha);
	}

	public UUID id() {
		return id;
	}

	public UUID empresaId() {
		return empresaId;
	}

	public String accion() {
		return accion;
	}

	public String detalle() {
		return detalle;
	}

	public Instant fecha() {
		return fecha;
	}
}
