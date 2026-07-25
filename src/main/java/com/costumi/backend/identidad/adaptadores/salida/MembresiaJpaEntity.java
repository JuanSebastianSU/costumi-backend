package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.EstadoMembresia;
import com.costumi.backend.identidad.dominio.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Mapeo JPA de la membresía persona↔tienda. <b>Sin</b> {@code @Filter} de tenant: es del USUARIO y cruza
 * empresas (para listar sus tiendas), no de una empresa.
 */
@Entity
@Table(name = "membresia")
class MembresiaJpaEntity {

	@Id
	private UUID id;

	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Rol rol;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoMembresia estado;

	protected MembresiaJpaEntity() {
		// requerido por JPA
	}

	MembresiaJpaEntity(UUID id, UUID usuarioId, UUID empresaId, Rol rol, EstadoMembresia estado) {
		this.id = id;
		this.usuarioId = usuarioId;
		this.empresaId = empresaId;
		this.rol = rol;
		this.estado = estado;
	}

	UUID getId() {
		return id;
	}

	UUID getUsuarioId() {
		return usuarioId;
	}

	UUID getEmpresaId() {
		return empresaId;
	}

	Rol getRol() {
		return rol;
	}

	EstadoMembresia getEstado() {
		return estado;
	}
}
