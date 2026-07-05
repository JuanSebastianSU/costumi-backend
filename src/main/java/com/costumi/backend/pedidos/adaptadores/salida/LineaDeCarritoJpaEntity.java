package com.costumi.backend.pedidos.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Mapeo JPA de una línea de carrito. */
@Entity
@Table(name = "linea_de_carrito")
@Filter(name = FiltroTenant.NOMBRE)
class LineaDeCarritoJpaEntity {

	@Id
	private UUID id;

	@Column(name = "carrito_id", nullable = false)
	private UUID carritoId;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(name = "prenda_id", nullable = false)
	private UUID prendaId;

	@Column(nullable = false)
	private int cantidad;

	protected LineaDeCarritoJpaEntity() {
		// requerido por JPA
	}

	LineaDeCarritoJpaEntity(UUID id, UUID carritoId, UUID empresaId, UUID prendaId, int cantidad) {
		this.id = id;
		this.carritoId = carritoId;
		this.empresaId = empresaId;
		this.prendaId = prendaId;
		this.cantidad = cantidad;
	}

	UUID getPrendaId() {
		return prendaId;
	}

	int getCantidad() {
		return cantidad;
	}
}
