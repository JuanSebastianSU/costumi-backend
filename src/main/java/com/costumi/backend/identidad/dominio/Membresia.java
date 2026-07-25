package com.costumi.backend.identidad.dominio;

import java.util.UUID;

/**
 * Membresía de una persona en una tienda (Fase B): su rol en esa empresa. Una persona puede tener varias
 * (una por tienda). Es aditiva al {@code Usuario} (que conserva su empresa+rol "base"); el cambio de
 * contexto emite un token con la empresa+rol de la membresía elegida.
 */
public record Membresia(UUID id, UUID usuarioId, UUID empresaId, Rol rol, EstadoMembresia estado) {

	public static Membresia crear(UUID usuarioId, UUID empresaId, Rol rol) {
		return new Membresia(UUID.randomUUID(), usuarioId, empresaId, rol, EstadoMembresia.ACTIVA);
	}

	public boolean activa() {
		return estado == EstadoMembresia.ACTIVA;
	}
}
