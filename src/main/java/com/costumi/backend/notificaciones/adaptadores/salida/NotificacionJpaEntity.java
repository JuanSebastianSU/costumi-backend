package com.costumi.backend.notificaciones.adaptadores.salida;

import com.costumi.backend.notificaciones.dominio.CanalNotificacion;
import com.costumi.backend.notificaciones.dominio.EstadoNotificacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Mapeo JPA de la Notificación. Lleva {@code empresa_id} (tenant). */
@Entity
@Table(name = "notificacion")
class NotificacionJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(name = "cliente_id")
	private UUID clienteId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private CanalNotificacion canal;

	@Column(nullable = false, length = 500)
	private String mensaje;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 12)
	private EstadoNotificacion estado;

	@Column(nullable = false)
	private Instant fecha;

	protected NotificacionJpaEntity() {
		// requerido por JPA
	}

	NotificacionJpaEntity(UUID id, UUID empresaId, UUID clienteId, CanalNotificacion canal, String mensaje,
			EstadoNotificacion estado, Instant fecha) {
		this.id = id;
		this.empresaId = empresaId;
		this.clienteId = clienteId;
		this.canal = canal;
		this.mensaje = mensaje;
		this.estado = estado;
		this.fecha = fecha;
	}

	UUID getId() {
		return id;
	}

	UUID getEmpresaId() {
		return empresaId;
	}

	UUID getClienteId() {
		return clienteId;
	}

	CanalNotificacion getCanal() {
		return canal;
	}

	String getMensaje() {
		return mensaje;
	}

	EstadoNotificacion getEstado() {
		return estado;
	}

	Instant getFecha() {
		return fecha;
	}
}
