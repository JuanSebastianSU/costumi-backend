package com.costumi.backend.rentas.adaptadores.entrada;

import com.costumi.backend.inventario.ConsultaDeInventario.ResumenDePrenda;
import com.costumi.backend.rentas.dominio.RentaLinea;

import java.math.BigDecimal;
import java.util.UUID;

/** DTO de salida de una línea de renta (un artículo), enriquecida con el nombre y la foto de la prenda. */
public record LineaRentaResponse(UUID prendaId, String nombre, String fotoUrl, int cantidad,
		BigDecimal precioPorDia) {

	static LineaRentaResponse desde(RentaLinea l, ResumenDePrenda resumen) {
		String nombre = resumen == null ? null : resumen.nombre();
		String fotoUrl = resumen == null ? null : resumen.fotoUrl();
		return new LineaRentaResponse(l.prendaId(), nombre, fotoUrl, l.cantidad(), l.precioPorDia());
	}
}
