package com.costumi.backend.identidad.dominio;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Invitación de trabajo a una persona (por email) a una tienda, con un rol y sus sucursales (Fase B, paso 3).
 * Es una entidad aparte de {@link Membresia} porque al invitar puede que la persona <b>aún no tenga cuenta</b>
 * (se crea al aceptar). Lleva un token de un solo uso (se guarda el hash, nunca el valor en claro) con
 * expiración, y registra la aceptación de términos y condiciones (decisión #3).
 */
public record Invitacion(UUID id, UUID empresaId, String email, Rol rol, Set<UUID> sucursalIds,
		String tokenHash, Instant expiraEn, EstadoInvitacion estado, Instant aceptoTerminosEn) {

	public static Invitacion crear(UUID empresaId, String email, Rol rol, Set<UUID> sucursalIds,
			String tokenHash, Instant expiraEn) {
		return new Invitacion(UUID.randomUUID(), empresaId, email, rol, Set.copyOf(sucursalIds),
				tokenHash, expiraEn, EstadoInvitacion.PENDIENTE, null);
	}

	public static Invitacion rehidratar(UUID id, UUID empresaId, String email, Rol rol, Set<UUID> sucursalIds,
			String tokenHash, Instant expiraEn, EstadoInvitacion estado, Instant aceptoTerminosEn) {
		return new Invitacion(id, empresaId, email, rol, Set.copyOf(sucursalIds), tokenHash, expiraEn, estado,
				aceptoTerminosEn);
	}

	/** Vigente = pendiente y no vencida (se puede aceptar). */
	public boolean vigente(Instant ahora) {
		return estado == EstadoInvitacion.PENDIENTE && ahora.isBefore(expiraEn);
	}

	/** La persona acepta (con T&C): queda ACEPTADA y se registra el momento de aceptación de los términos. */
	public Invitacion aceptar(Instant ahora) {
		return new Invitacion(id, empresaId, email, rol, sucursalIds, tokenHash, expiraEn,
				EstadoInvitacion.ACEPTADA, ahora);
	}

	/** El que invitó la cancela antes de que se acepte. */
	public Invitacion cancelar() {
		return new Invitacion(id, empresaId, email, rol, sucursalIds, tokenHash, expiraEn,
				EstadoInvitacion.CANCELADA, aceptoTerminosEn);
	}
}
