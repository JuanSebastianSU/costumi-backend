package com.costumi.backend.inventario.adaptadores.entrada;

import com.costumi.backend.inventario.dominio.PrendaDeCatalogo;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Prenda del catálogo del dueño: con su stock disponible y sus etiquetas, para verla dentro de su
 * categoría, filtrarla y elegirla como opción de una parte de un disfraz (RF-2/RF-13).
 */
record PrendaDeCatalogoResponse(UUID id, String nombre, UUID categoriaId, String tipoArticulo,
		BigDecimal precioRenta, BigDecimal precioVenta, BigDecimal valorDano, BigDecimal valorReposicion,
		String fotoUrl, int unidadesDisponibles, List<EtiquetaSeleccionadaDto> etiquetas) {

	static PrendaDeCatalogoResponse desde(PrendaDeCatalogo p) {
		List<EtiquetaSeleccionadaDto> etiquetas = p.etiquetas().entrySet().stream()
				.map(e -> new EtiquetaSeleccionadaDto(e.getKey(), e.getValue()))
				.toList();
		return new PrendaDeCatalogoResponse(p.id(), p.nombre(), p.categoriaId(), p.tipoArticulo(),
				p.precioRenta(), p.precioVenta(), p.valorDano(), p.valorReposicion(), p.fotoUrl(),
				p.unidadesDisponibles(), etiquetas);
	}
}
