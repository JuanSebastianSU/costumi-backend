package com.costumi.backend.notificaciones.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Notificación a un cliente (RF-11). Nace PENDIENTE y pasa a ENVIADA/FALLIDA según el canal.
 * Pertenece a una Empresa (tenant).
 */
public class Notificacion {

	private final UUID id;
	private final UUID empresaId;
	private final UUID clienteId;
	private final CanalNotificacion canal;
	private final String mensaje;
	private EstadoNotificacion estado;
	private final Instant fecha;

	private Notificacion(UUID id, UUID empresaId, UUID clienteId, CanalNotificacion canal, String mensaje,
			EstadoNotificacion estado, Instant fecha) {
		this.id = Objects.requireNonNull(id, "id");
		this.empresaId = Objects.requireNonNull(empresaId, "empresaId");
		this.clienteId = clienteId;
		this.canal = Objects.requireNonNull(canal, "canal");
		if (mensaje == null || mensaje.isBlank()) {
			throw new IllegalArgumentException("El mensaje de la notificación es obligatorio");
		}
		this.mensaje = mensaje.trim();
		this.estado = Objects.requireNonNull(estado, "estado");
		this.fecha = Objects.requireNonNull(fecha, "fecha");
	}

	public static Notificacion crear(UUID empresaId, UUID clienteId, CanalNotificacion canal, String mensaje) {
		return new Notificacion(UUID.randomUUID(), empresaId, clienteId, canal, mensaje,
				EstadoNotificacion.PENDIENTE, Instant.now());
	}

	public static Notificacion rehidratar(UUID id, UUID empresaId, UUID clienteId, CanalNotificacion canal,
			String mensaje, EstadoNotificacion estado, Instant fecha) {
		return new Notificacion(id, empresaId, clienteId, canal, mensaje, estado, fecha);
	}

	public void marcarEnviada() {
		this.estado = EstadoNotificacion.ENVIADA;
	}

	public void marcarFallida() {
		this.estado = EstadoNotificacion.FALLIDA;
	}

	public UUID id() {
		return id;
	}

	public UUID empresaId() {
		return empresaId;
	}

	public UUID clienteId() {
		return clienteId;
	}

	public CanalNotificacion canal() {
		return canal;
	}

	public String mensaje() {
		return mensaje;
	}

	public EstadoNotificacion estado() {
		return estado;
	}

	public Instant fecha() {
		return fecha;
	}
}
