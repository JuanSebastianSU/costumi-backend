package com.costumi.backend.rentas.adaptadores.entrada;

import com.costumi.backend.rentas.dominio.RentaLinea;

import java.math.BigDecimal;
import java.util.UUID;

/** DTO de salida de una línea de renta (un artículo). */
public record LineaRentaResponse(UUID prendaId, int cantidad, BigDecimal precioPorDia) {

	static LineaRentaResponse desde(RentaLinea l) {
		return new LineaRentaResponse(l.prendaId(), l.cantidad(), l.precioPorDia());
	}
}
