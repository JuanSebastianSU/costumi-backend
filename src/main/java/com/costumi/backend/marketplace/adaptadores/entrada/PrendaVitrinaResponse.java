package com.costumi.backend.marketplace.adaptadores.entrada;

import com.costumi.backend.marketplace.dominio.PrendaEnVitrina;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** DTO de salida de una prenda en el catálogo público de una tienda. */
public record PrendaVitrinaResponse(UUID id, String nombre, String tipoArticulo, BigDecimal precioRenta,
		BigDecimal precioVenta, String categoria, String fotoUrl, List<EtiquetaVitrinaDto> etiquetas) {

	/** Una etiqueta de la prenda por NOMBRE (tipo + valor), para que el cliente sepa qué está comprando. */
	public record EtiquetaVitrinaDto(String tipo, String valor) {
	}

	static PrendaVitrinaResponse desde(PrendaEnVitrina p) {
		List<EtiquetaVitrinaDto> etiquetas = p.etiquetas().stream()
				.map(e -> new EtiquetaVitrinaDto(e.tipo(), e.valor()))
				.toList();
		return new PrendaVitrinaResponse(p.id(), p.nombre(), p.tipoArticulo(), p.precioRenta(), p.precioVenta(),
				p.categoria(), p.fotoUrl(), etiquetas);
	}
}
