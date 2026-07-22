package com.costumi.backend.disfraces.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Mapeo JPA de la Categoría de disfraz. Lleva {@code empresa_id} (tenant). */
@Entity
@Table(name = "categoria_disfraz")
@Filter(name = FiltroTenant.NOMBRE)
class CategoriaDeDisfrazJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(nullable = false)
	private boolean archivada;

	protected CategoriaDeDisfrazJpaEntity() {
		// requerido por JPA
	}

	CategoriaDeDisfrazJpaEntity(UUID id, UUID empresaId, String nombre, boolean archivada) {
		this.id = id;
		this.empresaId = empresaId;
		this.nombre = nombre;
		this.archivada = archivada;
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

	boolean isArchivada() {
		return archivada;
	}
}
