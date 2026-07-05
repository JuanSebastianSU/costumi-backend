package com.costumi.backend.ventas.dominio;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Línea de una venta: una prenda, su cantidad y el precio unitario cobrado. */
public class LineaDeVenta {

	private final UUID prendaId;
	private final int cantidad;
	private final BigDecimal precioUnitario;

	private LineaDeVenta(UUID prendaId, int cantidad, BigDecimal precioUnitario) {
		this.prendaId = Objects.requireNonNull(prendaId, "prendaId");
		if (cantidad <= 0) {
			throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
		}
		if (precioUnitario == null || precioUnitario.signum() <= 0) {
			throw new IllegalArgumentException("El precio unitario debe ser mayor a 0");
		}
		this.cantidad = cantidad;
		this.precioUnitario = precioUnitario;
	}

	public static LineaDeVenta de(UUID prendaId, int cantidad, BigDecimal precioUnitario) {
		return new LineaDeVenta(prendaId, cantidad, precioUnitario);
	}

	public BigDecimal subtotal() {
		return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
	}

	public UUID prendaId() {
		return prendaId;
	}

	public int cantidad() {
		return cantidad;
	}

	public BigDecimal precioUnitario() {
		return precioUnitario;
	}
}
