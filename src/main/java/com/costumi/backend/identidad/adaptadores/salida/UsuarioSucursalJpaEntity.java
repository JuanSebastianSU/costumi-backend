package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Mapeo JPA de la asignación de un empleado a una sucursal (RF-1.2/8.1). */
@Entity
@Table(name = "usuario_sucursal")
@Filter(name = FiltroTenant.NOMBRE)
class UsuarioSucursalJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;

	@Column(name = "sucursal_id", nullable = false)
	private UUID sucursalId;

	protected UsuarioSucursalJpaEntity() {
		// requerido por JPA
	}

	UsuarioSucursalJpaEntity(UUID id, UUID empresaId, UUID usuarioId, UUID sucursalId) {
		this.id = id;
		this.empresaId = empresaId;
		this.usuarioId = usuarioId;
		this.sucursalId = sucursalId;
	}

	UUID getSucursalId() {
		return sucursalId;
	}
}
