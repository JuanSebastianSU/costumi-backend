package com.costumi.backend.catalogo.adaptadores.salida;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Mapeo JPA del Tipo de etiqueta. Lleva {@code empresa_id} (tenant). */
@Entity
@Table(name = "tipo_etiqueta")
class TipoEtiquetaJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(name = "define_variante", nullable = false)
	private boolean defineVariante;

	@Column(name = "seleccionable_cliente", nullable = false)
	private boolean seleccionableCliente;

	@Column(nullable = false)
	private boolean archivada;

	protected TipoEtiquetaJpaEntity() {
		// requerido por JPA
	}

	TipoEtiquetaJpaEntity(UUID id, UUID empresaId, String nombre, boolean defineVariante,
			boolean seleccionableCliente, boolean archivada) {
		this.id = id;
		this.empresaId = empresaId;
		this.nombre = nombre;
		this.defineVariante = defineVariante;
		this.seleccionableCliente = seleccionableCliente;
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

	boolean isDefineVariante() {
		return defineVariante;
	}

	boolean isSeleccionableCliente() {
		return seleccionableCliente;
	}

	boolean isArchivada() {
		return archivada;
	}
}
