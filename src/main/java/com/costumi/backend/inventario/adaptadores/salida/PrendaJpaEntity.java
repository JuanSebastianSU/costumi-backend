package com.costumi.backend.inventario.adaptadores.salida;

import com.costumi.backend.inventario.dominio.TipoArticulo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** Mapeo JPA de la Prenda. Lleva {@code empresa_id} (tenant) y la categoría. */
@Entity
@Table(name = "prenda")
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

	@Column(nullable = false)
	private boolean archivada;

	protected PrendaJpaEntity() {
		// requerido por JPA
	}

	PrendaJpaEntity(UUID id, UUID empresaId, UUID categoriaId, String nombre, TipoArticulo tipoArticulo,
			BigDecimal precioRenta, BigDecimal precioVenta, boolean archivada) {
		this.id = id;
		this.empresaId = empresaId;
		this.categoriaId = categoriaId;
		this.nombre = nombre;
		this.tipoArticulo = tipoArticulo;
		this.precioRenta = precioRenta;
		this.precioVenta = precioVenta;
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

	boolean isArchivada() {
		return archivada;
	}
}
