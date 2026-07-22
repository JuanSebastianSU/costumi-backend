package com.costumi.backend.pedidos.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Mapeo JPA de la elección de prenda para un slot del disfraz de una línea de carrito (RF-2.3/16). */
@Entity
@Table(name = "linea_de_carrito_seleccion")
@Filter(name = FiltroTenant.NOMBRE)
class LineaDeCarritoSeleccionJpaEntity {

	@Id
	private UUID id;

	@Column(name = "linea_id", nullable = false)
	private UUID lineaId;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(nullable = false)
	private int orden;

	@Column(name = "prenda_id", nullable = false)
	private UUID prendaId;

	protected LineaDeCarritoSeleccionJpaEntity() {
		// requerido por JPA
	}

	LineaDeCarritoSeleccionJpaEntity(UUID id, UUID lineaId, UUID empresaId, int orden, UUID prendaId) {
		this.id = id;
		this.lineaId = lineaId;
		this.empresaId = empresaId;
		this.orden = orden;
		this.prendaId = prendaId;
	}

	UUID getLineaId() {
		return lineaId;
	}

	int getOrden() {
		return orden;
	}

	UUID getPrendaId() {
		return prendaId;
	}
}
