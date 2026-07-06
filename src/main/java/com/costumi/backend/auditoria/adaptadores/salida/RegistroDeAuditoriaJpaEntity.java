package com.costumi.backend.auditoria.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

/** Mapeo JPA del registro de auditoría. Lleva {@code empresa_id} (tenant). */
@Entity
@Table(name = "registro_auditoria")
@Filter(name = FiltroTenant.NOMBRE)
class RegistroDeAuditoriaJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(nullable = false, length = 60)
	private String accion;

	@Column(length = 500)
	private String detalle;

	@Column(nullable = false)
	private Instant fecha;

	protected RegistroDeAuditoriaJpaEntity() {
		// requerido por JPA
	}

	RegistroDeAuditoriaJpaEntity(UUID id, UUID empresaId, String accion, String detalle, Instant fecha) {
		this.id = id;
		this.empresaId = empresaId;
		this.accion = accion;
		this.detalle = detalle;
		this.fecha = fecha;
	}

	UUID getId() {
		return id;
	}

	UUID getEmpresaId() {
		return empresaId;
	}

	String getAccion() {
		return accion;
	}

	String getDetalle() {
		return detalle;
	}

	Instant getFecha() {
		return fecha;
	}
}
