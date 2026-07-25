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

	/** Baja definitiva (por el dueño o por el empleado): ya no forma parte del personal. */
	public boolean esBaja() {
		return estado == EstadoMembresia.BAJA_DUENO || estado == EstadoMembresia.BAJA_EMPLEADO;
	}

	/** Misma membresía con otro rol (para mantenerla en sync cuando cambia el rol base del empleado). */
	public Membresia conRol(Rol nuevoRol) {
		return new Membresia(id, usuarioId, empresaId, nuevoRol, estado);
	}

	/** El dueño la suspende (corta el acceso al toque; reversible con {@link #reactivar()}). */
	public Membresia suspender() {
		return con(EstadoMembresia.SUSPENDIDA);
	}

	/** El dueño la reactiva tras suspenderla. */
	public Membresia reactivar() {
		return con(EstadoMembresia.ACTIVA);
	}

	/** El dueño da de baja al empleado (despido/quita): definitivo, se vuelve re-invitando. */
	public Membresia darDeBajaPorDueno() {
		return con(EstadoMembresia.BAJA_DUENO);
	}

	/** El empleado se desvincula por su cuenta: definitivo, para volver hace falta re-invitación. */
	public Membresia darDeBajaPorEmpleado() {
		return con(EstadoMembresia.BAJA_EMPLEADO);
	}

	private Membresia con(EstadoMembresia nuevoEstado) {
		return new Membresia(id, usuarioId, empresaId, rol, nuevoEstado);
	}
}
