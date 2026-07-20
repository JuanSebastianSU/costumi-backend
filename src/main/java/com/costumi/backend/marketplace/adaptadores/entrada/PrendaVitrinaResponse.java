package com.costumi.backend.marketplace.adaptadores.entrada;

import com.costumi.backend.marketplace.dominio.PrendaEnVitrina;

import java.math.BigDecimal;
import java.util.UUID;

/** DTO de salida de una prenda en el catálogo público de una tienda. */
public record PrendaVitrinaResponse(UUID id, String nombre, String tipoArticulo, BigDecimal precioRenta,
		BigDecimal precioVenta, String categoria, String fotoUrl) {

	static PrendaVitrinaResponse desde(PrendaEnVitrina p) {
		return new PrendaVitrinaResponse(p.id(), p.nombre(), p.tipoArticulo(), p.precioRenta(), p.precioVenta(),
				p.categoria(), p.fotoUrl());
	}
}
