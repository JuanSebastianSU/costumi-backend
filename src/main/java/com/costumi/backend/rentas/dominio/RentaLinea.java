package com.costumi.backend.rentas.dominio;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Línea de una renta (RF-3.1/3.6): un artículo (prenda), su cantidad y el precio por día pactado.
 * Una renta lleva una o más líneas (multi-artículo).
 */
public class RentaLinea {

	private final UUID prendaId;
	private final int cantidad;
	private final BigDecimal precioPorDia;
	/** De qué disfraz salió esta línea, o null si es una prenda suelta. */
	private final OrigenDisfraz origenDisfraz;

	private RentaLinea(UUID prendaId, int cantidad, BigDecimal precioPorDia, OrigenDisfraz origenDisfraz) {
		this.prendaId = Objects.requireNonNull(prendaId, "prendaId");
		if (cantidad <= 0) {
			throw new IllegalArgumentException("La cantidad de la línea debe ser mayor a 0");
		}
		if (precioPorDia == null || precioPorDia.signum() <= 0) {
			throw new IllegalArgumentException("El precio por día debe ser mayor a 0");
		}
		this.cantidad = cantidad;
		this.precioPorDia = precioPorDia;
		this.origenDisfraz = origenDisfraz;
	}

	public static RentaLinea de(UUID prendaId, int cantidad, BigDecimal precioPorDia) {
		return new RentaLinea(prendaId, cantidad, precioPorDia, null);
	}

	/** Línea que salió de armar un disfraz: recuerda cuál, para no perderlo al cobrar. */
	public static RentaLinea de(UUID prendaId, int cantidad, BigDecimal precioPorDia,
			OrigenDisfraz origenDisfraz) {
		return new RentaLinea(prendaId, cantidad, precioPorDia, origenDisfraz);
	}

	public OrigenDisfraz origenDisfraz() {
		return origenDisfraz;
	}

	/** Subtotal por día de la línea (precio × cantidad); el importe multiplica esto por el periodo. */
	public BigDecimal subtotalPorDia() {
		return precioPorDia.multiply(BigDecimal.valueOf(cantidad));
	}

	public UUID prendaId() {
		return prendaId;
	}

	public int cantidad() {
		return cantidad;
	}

	public BigDecimal precioPorDia() {
		return precioPorDia;
	}
}
