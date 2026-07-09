package com.costumi.backend.ventas.dominio;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Línea de una venta: una prenda, su cantidad y el precio unitario cobrado. Lleva además cuántas
 * unidades de esta línea ya se <b>devolvieron</b> ({@code cantidadDevuelta}), para admitir reembolsos
 * parciales (RF-4.5): la venta se devuelve por partes hasta completar todas sus líneas.
 */
public class LineaDeVenta {

	private final UUID prendaId;
	private final int cantidad;
	private final BigDecimal precioUnitario;
	private int cantidadDevuelta;

	private LineaDeVenta(UUID prendaId, int cantidad, BigDecimal precioUnitario, int cantidadDevuelta) {
		this.prendaId = Objects.requireNonNull(prendaId, "prendaId");
		if (cantidad <= 0) {
			throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
		}
		if (precioUnitario == null || precioUnitario.signum() <= 0) {
			throw new IllegalArgumentException("El precio unitario debe ser mayor a 0");
		}
		if (cantidadDevuelta < 0 || cantidadDevuelta > cantidad) {
			throw new IllegalArgumentException("La cantidad devuelta debe estar entre 0 y la cantidad de la línea");
		}
		this.cantidad = cantidad;
		this.precioUnitario = precioUnitario;
		this.cantidadDevuelta = cantidadDevuelta;
	}

	public static LineaDeVenta de(UUID prendaId, int cantidad, BigDecimal precioUnitario) {
		return new LineaDeVenta(prendaId, cantidad, precioUnitario, 0);
	}

	public static LineaDeVenta rehidratar(UUID prendaId, int cantidad, BigDecimal precioUnitario,
			int cantidadDevuelta) {
		return new LineaDeVenta(prendaId, cantidad, precioUnitario, cantidadDevuelta);
	}

	/** Devuelve {@code cantidad} unidades de esta línea; no puede exceder lo pendiente. */
	void devolver(int cantidad) {
		if (cantidad <= 0) {
			throw new IllegalArgumentException("La cantidad a devolver debe ser mayor a 0");
		}
		if (cantidad > pendiente()) {
			throw new IllegalArgumentException("Se devuelven más unidades de las pendientes en la línea");
		}
		this.cantidadDevuelta += cantidad;
	}

	public int pendiente() {
		return cantidad - cantidadDevuelta;
	}

	public boolean estaTotalmenteDevuelta() {
		return cantidadDevuelta >= cantidad;
	}

	public BigDecimal subtotal() {
		return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
	}

	/** Valor (a precio de lista) de las unidades ya devueltas de esta línea. */
	public BigDecimal subtotalDevuelto() {
		return precioUnitario.multiply(BigDecimal.valueOf(cantidadDevuelta));
	}

	public UUID prendaId() {
		return prendaId;
	}

	public int cantidad() {
		return cantidad;
	}

	public int cantidadDevuelta() {
		return cantidadDevuelta;
	}

	public BigDecimal precioUnitario() {
		return precioUnitario;
	}
}
