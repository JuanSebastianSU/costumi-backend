package com.costumi.backend.identidad.adaptadores.salida;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Mapeo JPA del token de recuperación de contraseña. Nunca se expone por la API. */
@Entity
@Table(name = "token_recuperacion")
class TokenRecuperacionJpaEntity {

	@Id
	private UUID id;

	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;

	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "expira_en", nullable = false)
	private Instant expiraEn;

	@Column(nullable = false)
	private boolean usado;

	protected TokenRecuperacionJpaEntity() {
		// requerido por JPA
	}

	TokenRecuperacionJpaEntity(UUID id, UUID usuarioId, String tokenHash, Instant expiraEn, boolean usado) {
		this.id = id;
		this.usuarioId = usuarioId;
		this.tokenHash = tokenHash;
		this.expiraEn = expiraEn;
		this.usado = usado;
	}

	UUID getId() {
		return id;
	}

	UUID getUsuarioId() {
		return usuarioId;
	}

	String getTokenHash() {
		return tokenHash;
	}

	Instant getExpiraEn() {
		return expiraEn;
	}

	boolean isUsado() {
		return usado;
	}
}
