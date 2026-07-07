package com.costumi.backend.identidad.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Token de recuperación de contraseña (RF-1.1). Se guarda solo el <b>hash</b> del token (el valor
 * en claro viaja por email y nunca se persiste). Un solo uso y con vencimiento.
 */
public class TokenDeRecuperacion {

	private final UUID id;
	private final UUID usuarioId;
	private final String tokenHash;
	private final Instant expiraEn;
	private boolean usado;

	private TokenDeRecuperacion(UUID id, UUID usuarioId, String tokenHash, Instant expiraEn, boolean usado) {
		this.id = Objects.requireNonNull(id, "id");
		this.usuarioId = Objects.requireNonNull(usuarioId, "usuarioId");
		this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
		this.expiraEn = Objects.requireNonNull(expiraEn, "expiraEn");
		this.usado = usado;
	}

	public static TokenDeRecuperacion crear(UUID usuarioId, String tokenHash, Instant expiraEn) {
		return new TokenDeRecuperacion(UUID.randomUUID(), usuarioId, tokenHash, expiraEn, false);
	}

	public static TokenDeRecuperacion rehidratar(UUID id, UUID usuarioId, String tokenHash, Instant expiraEn,
			boolean usado) {
		return new TokenDeRecuperacion(id, usuarioId, tokenHash, expiraEn, usado);
	}

	/** Vigente = no usado y no vencido. */
	public boolean esVigente(Instant ahora) {
		return !usado && ahora.isBefore(expiraEn);
	}

	public void marcarUsado() {
		this.usado = true;
	}

	public UUID id() {
		return id;
	}

	public UUID usuarioId() {
		return usuarioId;
	}

	public String tokenHash() {
		return tokenHash;
	}

	public Instant expiraEn() {
		return expiraEn;
	}

	public boolean usado() {
		return usado;
	}
}
