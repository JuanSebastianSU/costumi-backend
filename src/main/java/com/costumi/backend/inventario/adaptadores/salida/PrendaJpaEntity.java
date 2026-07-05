package com.costumi.backend.inventario.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import com.costumi.backend.inventario.dominio.TipoArticulo;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Mapeo JPA de la Prenda. Lleva {@code empresa_id} (tenant) y la categoría. */
@Entity
@Table(name = "prenda")
@Filter(name = FiltroTenant.NOMBRE)
class PrendaJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(name = "categoria_id", nullable = false)
	private UUID categoriaId;

	@Column(nullable = false, length = 160)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_articulo", nullable = false, length = 10)
	private TipoArticulo tipoArticulo;

	@Column(name = "precio_renta", precision = 12, scale = 2)
	private BigDecimal precioRenta;

	@Column(name = "precio_venta", precision = 12, scale = 2)
	private BigDecimal precioVenta;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "prenda_valor_etiqueta", joinColumns = @JoinColumn(name = "prenda_id"))
	private Set<EtiquetaDePrendaEmbeddable> etiquetas = new LinkedHashSet<>();

	@Column(nullable = false)
	private boolean archivada;

	protected PrendaJpaEntity() {
		// requerido por JPA
	}

	PrendaJpaEntity(UUID id, UUID empresaId, UUID categoriaId, String nombre, TipoArticulo tipoArticulo,
			BigDecimal precioRenta, BigDecimal precioVenta, Set<EtiquetaDePrendaEmbeddable> etiquetas, boolean archivada) {
		this.id = id;
		this.empresaId = empresaId;
		this.categoriaId = categoriaId;
		this.nombre = nombre;
		this.tipoArticulo = tipoArticulo;
		this.precioRenta = precioRenta;
		this.precioVenta = precioVenta;
		this.etiquetas = etiquetas;
		this.archivada = archivada;
	}

	UUID getId() {
		return id;
	}

	UUID getEmpresaId() {
		return empresaId;
	}

	UUID getCategoriaId() {
		return categoriaId;
	}

	String getNombre() {
		return nombre;
	}

	TipoArticulo getTipoArticulo() {
		return tipoArticulo;
	}

	BigDecimal getPrecioRenta() {
		return precioRenta;
	}

	BigDecimal getPrecioVenta() {
		return precioVenta;
	}

	Set<EtiquetaDePrendaEmbeddable> getEtiquetas() {
		return etiquetas;
	}

	boolean isArchivada() {
		return archivada;
	}
}
