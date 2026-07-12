package com.costumi.backend.identidad.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro server-side de un token de refresco emitido (C2). Su {@code jti} es el identificador del JWT;
 * la {@code familiaId} agrupa la cadena de rotaciones de una misma sesión. Con la rotación (cada uso emite
 * un nuevo refresco y marca el anterior como {@code ROTADO}) se puede <b>revocar</b> y <b>detectar reuso</b>:
 * si llega un refresco ya {@code ROTADO}/{@code REVOCADO}, es señal de robo y se anula toda la familia.
 */
public class TokenDeRefresh {

	private final UUID jti;
	private final UUID usuarioId;
	private final UUID familiaId;
	private EstadoDeRefresh estado;
	private final Instant expiraEn;
	private final Instant creadoEn;

	private TokenDeRefresh(UUID jti, UUID usuarioId, UUID familiaId, EstadoDeRefresh estado, Instant expiraEn,
			Instant creadoEn) {
		this.jti = Objects.requireNonNull(jti, "jti");
		this.usuarioId = Objects.requireNonNull(usuarioId, "usuarioId");
		this.familiaId = Objects.requireNonNull(familiaId, "familiaId");
		this.estado = Objects.requireNonNull(estado, "estado");
		this.expiraEn = Objects.requireNonNull(expiraEn, "expiraEn");
		this.creadoEn = Objects.requireNonNull(creadoEn, "creadoEn");
	}

	public static TokenDeRefresh crear(UUID usuarioId, UUID jti, UUID familiaId, Instant expiraEn, Instant creadoEn) {
		return new TokenDeRefresh(jti, usuarioId, familiaId, EstadoDeRefresh.ACTIVO, expiraEn, creadoEn);
	}

	public static TokenDeRefresh rehidratar(UUID jti, UUID usuarioId, UUID familiaId, EstadoDeRefresh estado,
			Instant expiraEn, Instant creadoEn) {
		return new TokenDeRefresh(jti, usuarioId, familiaId, estado, expiraEn, creadoEn);
	}

	/** Solo un refresco ACTIVO puede rotarse; cualquier otro estado presentado es reuso. */
	public boolean esActivo() {
		return estado == EstadoDeRefresh.ACTIVO;
	}

	/** Marca este refresco como consumido por una rotación exitosa. */
	public void marcarRotado() {
		this.estado = EstadoDeRefresh.ROTADO;
	}

	public UUID jti() {
		return jti;
	}

	public UUID usuarioId() {
		return usuarioId;
	}

	public UUID familiaId() {
		return familiaId;
	}

	public EstadoDeRefresh estado() {
		return estado;
	}

	public Instant expiraEn() {
		return expiraEn;
	}

	public Instant creadoEn() {
		return creadoEn;
	}
}
