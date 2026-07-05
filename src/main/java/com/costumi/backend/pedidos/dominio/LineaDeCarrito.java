package com.costumi.backend.pedidos.dominio;

import java.util.Objects;
import java.util.UUID;

/** Línea de un carrito: una prenda y su cantidad. */
public class LineaDeCarrito {

	private final UUID prendaId;
	private int cantidad;

	private LineaDeCarrito(UUID prendaId, int cantidad) {
		this.prendaId = Objects.requireNonNull(prendaId, "prendaId");
		if (cantidad <= 0) {
			throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
		}
		this.cantidad = cantidad;
	}

	public static LineaDeCarrito de(UUID prendaId, int cantidad) {
		return new LineaDeCarrito(prendaId, cantidad);
	}

	void incrementar(int cantidad) {
		if (cantidad <= 0) {
			throw new IllegalArgumentException("La cantidad a agregar debe ser mayor a 0");
		}
		this.cantidad += cantidad;
	}

	public UUID prendaId() {
		return prendaId;
	}

	public int cantidad() {
		return cantidad;
	}
}
