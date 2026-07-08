package com.costumi.backend.pedidos.dominio;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Línea de un carrito: una prenda y su cantidad. En los carritos de RENTA lleva además su periodo
 * (retiro/devolución) — cada artículo puede tener fechas propias (RF-18.6). En los de VENTA las fechas
 * son nulas.
 */
public class LineaDeCarrito {

	private final UUID prendaId;
	private int cantidad;
	private final LocalDate fechaRetiro;
	private final LocalDate fechaDevolucion;

	private LineaDeCarrito(UUID prendaId, int cantidad, LocalDate fechaRetiro, LocalDate fechaDevolucion) {
		this.prendaId = Objects.requireNonNull(prendaId, "prendaId");
		if (cantidad <= 0) {
			throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
		}
		if (fechaRetiro != null && fechaDevolucion != null && fechaDevolucion.isBefore(fechaRetiro)) {
			throw new IllegalArgumentException("La fecha de devolución no puede ser anterior a la de retiro");
		}
		this.cantidad = cantidad;
		this.fechaRetiro = fechaRetiro;
		this.fechaDevolucion = fechaDevolucion;
	}

	public static LineaDeCarrito de(UUID prendaId, int cantidad) {
		return new LineaDeCarrito(prendaId, cantidad, null, null);
	}

	public static LineaDeCarrito de(UUID prendaId, int cantidad, LocalDate fechaRetiro, LocalDate fechaDevolucion) {
		return new LineaDeCarrito(prendaId, cantidad, fechaRetiro, fechaDevolucion);
	}

	void incrementar(int cantidad) {
		if (cantidad <= 0) {
			throw new IllegalArgumentException("La cantidad a agregar debe ser mayor a 0");
		}
		this.cantidad += cantidad;
	}

	/** ¿Esta línea corresponde al mismo artículo y periodo (clave de agrupación en el carrito)? */
	boolean mismaClave(UUID prendaId, LocalDate fechaRetiro, LocalDate fechaDevolucion) {
		return this.prendaId.equals(prendaId)
				&& Objects.equals(this.fechaRetiro, fechaRetiro)
				&& Objects.equals(this.fechaDevolucion, fechaDevolucion);
	}

	public UUID prendaId() {
		return prendaId;
	}

	public int cantidad() {
		return cantidad;
	}

	public LocalDate fechaRetiro() {
		return fechaRetiro;
	}

	public LocalDate fechaDevolucion() {
		return fechaDevolucion;
	}
}
