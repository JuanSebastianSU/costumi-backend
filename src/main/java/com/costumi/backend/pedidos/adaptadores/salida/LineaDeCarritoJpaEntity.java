package com.costumi.backend.pedidos.adaptadores.salida;

import com.costumi.backend.compartido.FiltroTenant;
import org.hibernate.annotations.Filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Mapeo JPA de una línea de carrito: una prenda ({@code prenda_id}) o un disfraz ({@code disfraz_id}),
 * nunca ambos (CHECK en la BD). Las fechas solo aplican a los carritos de RENTA (RF-18.6). La elección
 * de prenda por slot de un disfraz vive en {@link LineaDeCarritoSeleccionJpaEntity}.
 */
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

	@Column(name = "prenda_id")
	private UUID prendaId;

	@Column(name = "disfraz_id")
	private UUID disfrazId;

	@Column(nullable = false)
	private int cantidad;

	@Column(name = "fecha_retiro")
	private LocalDate fechaRetiro;

	@Column(name = "fecha_devolucion")
	private LocalDate fechaDevolucion;

	protected LineaDeCarritoJpaEntity() {
		// requerido por JPA
	}

	LineaDeCarritoJpaEntity(UUID id, UUID carritoId, UUID empresaId, UUID prendaId, UUID disfrazId, int cantidad,
			LocalDate fechaRetiro, LocalDate fechaDevolucion) {
		this.id = id;
		this.carritoId = carritoId;
		this.empresaId = empresaId;
		this.prendaId = prendaId;
		this.disfrazId = disfrazId;
		this.cantidad = cantidad;
		this.fechaRetiro = fechaRetiro;
		this.fechaDevolucion = fechaDevolucion;
	}

	UUID getId() {
		return id;
	}

	UUID getPrendaId() {
		return prendaId;
	}

	UUID getDisfrazId() {
		return disfrazId;
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
