package com.costumi.backend.catalogo.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Mapeo JPA del Tipo de etiqueta. Lleva {@code empresa_id} (tenant) y a qué categorías aplica. */
@Entity
@Table(name = "tipo_etiqueta")
@Filter(name = FiltroTenant.NOMBRE)
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

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "tipo_etiqueta_categoria", joinColumns = @JoinColumn(name = "tipo_etiqueta_id"))
	@Column(name = "categoria_id", nullable = false)
	private Set<UUID> categoriasQueAplica = new LinkedHashSet<>();

	@Column(nullable = false)
	private boolean archivada;

	protected TipoEtiquetaJpaEntity() {
		// requerido por JPA
	}

	TipoEtiquetaJpaEntity(UUID id, UUID empresaId, String nombre, boolean defineVariante,
			boolean seleccionableCliente, Set<UUID> categoriasQueAplica, boolean archivada) {
		this.id = id;
		this.empresaId = empresaId;
		this.nombre = nombre;
		this.defineVariante = defineVariante;
		this.seleccionableCliente = seleccionableCliente;
		this.categoriasQueAplica = categoriasQueAplica;
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

	Set<UUID> getCategoriasQueAplica() {
		return categoriasQueAplica;
	}

	boolean isArchivada() {
		return archivada;
	}
}
