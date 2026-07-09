package com.costumi.backend.disfraces.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Mapeo JPA de la cabecera del Disfraz. Lleva {@code empresa_id} (tenant). */
@Entity
@Table(name = "disfraz")
@Filter(name = FiltroTenant.NOMBRE)
class DisfrazJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(nullable = false, length = 160)
	private String nombre;

	@Column(nullable = false)
	private boolean activo;

	protected DisfrazJpaEntity() {
		// requerido por JPA
	}

	DisfrazJpaEntity(UUID id, UUID empresaId, String nombre, boolean activo) {
		this.id = id;
		this.empresaId = empresaId;
		this.nombre = nombre;
		this.activo = activo;
	}

	UUID getId() {
		return id;
	}

	UUID getEmpresaId() {
		return empresaId;
	}

	String getNombre() {
		return nombre;
	}

	boolean isActivo() {
		return activo;
	}
}
