package com.costumi.backend.marketplace.adaptadores.entrada;

import com.costumi.backend.marketplace.dominio.CategoriaEnVitrina;

import java.util.UUID;

/** DTO de salida de una faceta (categoría) del catálogo público de una tienda (RF-18.1). */
public record CategoriaVitrinaResponse(UUID id, String nombre) {

	static CategoriaVitrinaResponse desde(CategoriaEnVitrina c) {
		return new CategoriaVitrinaResponse(c.id(), c.nombre());
	}
}
