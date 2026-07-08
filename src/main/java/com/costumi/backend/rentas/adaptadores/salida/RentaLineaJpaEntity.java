package com.costumi.backend.rentas.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** Mapeo JPA de una línea de renta (un artículo con su cantidad y precio por día). */
@Entity
@Table(name = "renta_linea")
@Filter(name = FiltroTenant.NOMBRE)
class RentaLineaJpaEntity {

	@Id
	private UUID id;

	@Column(name = "renta_id", nullable = false)
	private UUID rentaId;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(name = "prenda_id", nullable = false)
	private UUID prendaId;

	@Column(nullable = false)
	private int cantidad;

	@Column(name = "precio_por_dia", nullable = false, precision = 12, scale = 2)
	private BigDecimal precioPorDia;

	protected RentaLineaJpaEntity() {
		// requerido por JPA
	}

	RentaLineaJpaEntity(UUID id, UUID rentaId, UUID empresaId, UUID prendaId, int cantidad, BigDecimal precioPorDia) {
		this.id = id;
		this.rentaId = rentaId;
		this.empresaId = empresaId;
		this.prendaId = prendaId;
		this.cantidad = cantidad;
		this.precioPorDia = precioPorDia;
	}

	UUID getPrendaId() {
		return prendaId;
	}

	int getCantidad() {
		return cantidad;
	}

	BigDecimal getPrecioPorDia() {
		return precioPorDia;
	}
}
