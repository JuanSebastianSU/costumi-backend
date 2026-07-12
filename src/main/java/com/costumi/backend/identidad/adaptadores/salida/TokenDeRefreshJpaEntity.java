package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.EstadoDeRefresh;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Mapeo JPA del registro server-side de un refresco (C2). Nunca se expone por la API. */
@Entity
@Table(name = "token_refresh")
class TokenDeRefreshJpaEntity {

	@Id
	private UUID jti;

	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;

	@Column(name = "familia_id", nullable = false)
	private UUID familiaId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoDeRefresh estado;

	@Column(name = "expira_en", nullable = false)
	private Instant expiraEn;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	protected TokenDeRefreshJpaEntity() {
		// requerido por JPA
	}

	TokenDeRefreshJpaEntity(UUID jti, UUID usuarioId, UUID familiaId, EstadoDeRefresh estado, Instant expiraEn,
			Instant creadoEn) {
		this.jti = jti;
		this.usuarioId = usuarioId;
		this.familiaId = familiaId;
		this.estado = estado;
		this.expiraEn = expiraEn;
		this.creadoEn = creadoEn;
	}

	UUID getJti() {
		return jti;
	}

	UUID getUsuarioId() {
		return usuarioId;
	}

	UUID getFamiliaId() {
		return familiaId;
	}

	EstadoDeRefresh getEstado() {
		return estado;
	}

	Instant getExpiraEn() {
		return expiraEn;
	}

	Instant getCreadoEn() {
		return creadoEn;
	}
}
