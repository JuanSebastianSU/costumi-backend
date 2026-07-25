package com.costumi.backend.identidad.dominio;

import java.util.UUID;

/**
 * Membresía de trabajo de una persona en una tienda (Fase B): su rol en esa empresa. Regla de seguridad #2:
 * <b>una persona tiene a lo sumo UNA membresía de trabajo ACTIVA</b> (no se trabaja para dos tiendas a la
 * vez). Es aditiva al {@code Usuario}; el cambio de contexto («Trabajando») emite un token con la empresa+rol
 * de esta membresía, y «Comprando» emite un token de cliente.
 */
public record Membresia(UUID id, UUID usuarioId, UUID empresaId, Rol rol, EstadoMembresia estado) {

	public static Membresia crear(UUID usuarioId, UUID empresaId, Rol rol) {
		return new Membresia(UUID.randomUUID(), usuarioId, empresaId, rol, EstadoMembresia.ACTIVA);
	}

	public boolean activa() {
		return estado == EstadoMembresia.ACTIVA;
	}

	/** Misma membresía con otro rol (para mantenerla en sync cuando cambia el rol base del empleado). */
	public Membresia conRol(Rol nuevoRol) {
		return new Membresia(id, usuarioId, empresaId, nuevoRol, estado);
	}
}
