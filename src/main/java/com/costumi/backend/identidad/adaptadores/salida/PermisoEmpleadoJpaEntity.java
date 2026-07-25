package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Mapeo JPA de un override de capacidad por empleado (Fase B, paso 5): una capacidad concedida/negada por
 * encima del preset del rol. Su ausencia = se usa el preset del rol. {@code capacidad} guarda el nombre del
 * enum {@code Capacidad}.
 */
@Entity
@Table(name = "permiso_empleado")
@Filter(name = FiltroTenant.NOMBRE)
class PermisoEmpleadoJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;

	@Column(nullable = false, length = 50)
	private String capacidad;

	@Column(nullable = false)
	private boolean concedido;

	protected PermisoEmpleadoJpaEntity() {
		// requerido por JPA
	}

	PermisoEmpleadoJpaEntity(UUID id, UUID empresaId, UUID usuarioId, String capacidad, boolean concedido) {
		this.id = id;
		this.empresaId = empresaId;
		this.usuarioId = usuarioId;
		this.capacidad = capacidad;
		this.concedido = concedido;
	}

	UUID getId() {
		return id;
	}

	String getCapacidad() {
		return capacidad;
	}

	boolean isConcedido() {
		return concedido;
	}

	void setConcedido(boolean concedido) {
		this.concedido = concedido;
	}
}
