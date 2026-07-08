package com.costumi.backend.pedidos.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/** Mapeo JPA de una línea de carrito. Las fechas solo aplican a los carritos de RENTA (RF-18.6). */
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

	@Column(name = "fecha_retiro")
	private LocalDate fechaRetiro;

	@Column(name = "fecha_devolucion")
	private LocalDate fechaDevolucion;

	protected LineaDeCarritoJpaEntity() {
		// requerido por JPA
	}

	LineaDeCarritoJpaEntity(UUID id, UUID carritoId, UUID empresaId, UUID prendaId, int cantidad,
			LocalDate fechaRetiro, LocalDate fechaDevolucion) {
		this.id = id;
		this.carritoId = carritoId;
		this.empresaId = empresaId;
		this.prendaId = prendaId;
		this.cantidad = cantidad;
		this.fechaRetiro = fechaRetiro;
		this.fechaDevolucion = fechaDevolucion;
	}

	UUID getPrendaId() {
		return prendaId;
	}

	int getCantidad() {
		return cantidad;
	}

	LocalDate getFechaRetiro() {
		return fechaRetiro;
	}

	LocalDate getFechaDevolucion() {
		return fechaDevolucion;
	}
}
