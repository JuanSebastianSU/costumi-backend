package com.costumi.backend.inventario.dominio;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Fila de lectura del catálogo de inventario del dueño (RF-2/RF-13): una prenda con su stock
 * disponible (suma de sus grupos de stock) y sus etiquetas ({@code tipoEtiquetaId -> valorEtiquetaId}).
 * Sirve para verla dentro de su categoría, filtrarla por etiqueta, y elegirla como opción de una parte
 * de un disfraz. Es un modelo de lectura, no el agregado {@link Prenda}.
 */
public record PrendaDeCatalogo(UUID id, String nombre, UUID categoriaId, String tipoArticulo,
		BigDecimal precioRenta, BigDecimal precioVenta, BigDecimal valorDano, BigDecimal valorReposicion,
		String fotoUrl, int unidadesDisponibles, Map<UUID, UUID> etiquetas) {
}
