package com.costumi.backend.ventas.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** Mapeo JPA de una línea de venta. */
@Entity
@Table(name = "linea_de_venta")
@Filter(name = FiltroTenant.NOMBRE)
class LineaDeVentaJpaEntity {

	@Id
	private UUID id;

	@Column(name = "venta_id", nullable = false)
	private UUID ventaId;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(name = "prenda_id", nullable = false)
	private UUID prendaId;

	@Column(nullable = false)
	private int cantidad;

	@Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
	private BigDecimal precioUnitario;

	@Column(name = "cantidad_devuelta", nullable = false)
	private int cantidadDevuelta;

	/** De qué disfraz salió la línea (null si es una prenda suelta). Las tres viajan juntas. */
	@Column(name = "disfraz_id")
	private UUID disfrazId;

	@Column(name = "disfraz_grupo")
	private UUID disfrazGrupo;

	@Column(name = "disfraz_cantidad")
	private Integer disfrazCantidad;

	/** Nombre con el que se cobró el disfraz (histórico: no cambia si después lo renombran). */
	@Column(name = "disfraz_nombre")
	private String disfrazNombre;

	protected LineaDeVentaJpaEntity() {
		// requerido por JPA
	}

	LineaDeVentaJpaEntity(UUID id, UUID ventaId, UUID empresaId, UUID prendaId, int cantidad,
			BigDecimal precioUnitario, int cantidadDevuelta) {
		this(id, ventaId, empresaId, prendaId, cantidad, precioUnitario, cantidadDevuelta, null, null, null, null);
	}

	LineaDeVentaJpaEntity(UUID id, UUID ventaId, UUID empresaId, UUID prendaId, int cantidad,
			BigDecimal precioUnitario, int cantidadDevuelta, UUID disfrazId, UUID disfrazGrupo,
			Integer disfrazCantidad, String disfrazNombre) {
		this.id = id;
		this.ventaId = ventaId;
		this.empresaId = empresaId;
		this.prendaId = prendaId;
		this.cantidad = cantidad;
		this.precioUnitario = precioUnitario;
		this.cantidadDevuelta = cantidadDevuelta;
		this.disfrazId = disfrazId;
		this.disfrazGrupo = disfrazGrupo;
		this.disfrazCantidad = disfrazCantidad;
		this.disfrazNombre = disfrazNombre;
	}

	UUID getDisfrazId() {
		return disfrazId;
	}

	UUID getDisfrazGrupo() {
		return disfrazGrupo;
	}

	Integer getDisfrazCantidad() {
		return disfrazCantidad;
	}

	String getDisfrazNombre() {
		return disfrazNombre;
	}

	UUID getVentaId() {
		return ventaId;
	}

	UUID getPrendaId() {
		return prendaId;
	}

	int getCantidad() {
		return cantidad;
	}

	BigDecimal getPrecioUnitario() {
		return precioUnitario;
	}

	int getCantidadDevuelta() {
		return cantidadDevuelta;
	}
}
