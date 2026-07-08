package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Mapeo JPA de un override de permiso por empleado (RF-1.5): una casilla activada/desactivada por el
 * dueño encima de la plantilla del rol. Su ausencia = se usa el valor por defecto del rol.
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

	@Column(nullable = false, length = 20)
	private String seccion;

	@Column(nullable = false, length = 10)
	private String accion;

	@Column(nullable = false)
	private boolean concedido;

	protected PermisoEmpleadoJpaEntity() {
		// requerido por JPA
	}

	PermisoEmpleadoJpaEntity(UUID id, UUID empresaId, UUID usuarioId, String seccion, String accion,
			boolean concedido) {
		this.id = id;
		this.empresaId = empresaId;
		this.usuarioId = usuarioId;
		this.seccion = seccion;
		this.accion = accion;
		this.concedido = concedido;
	}

	UUID getId() {
		return id;
	}

	String getSeccion() {
		return seccion;
	}

	String getAccion() {
		return accion;
	}

	boolean isConcedido() {
		return concedido;
	}

	void setConcedido(boolean concedido) {
		this.concedido = concedido;
	}
}
