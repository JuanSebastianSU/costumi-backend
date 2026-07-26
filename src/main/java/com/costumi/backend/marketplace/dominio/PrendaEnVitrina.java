package com.costumi.backend.marketplace.dominio;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Modelo de lectura de una prenda visible en el catálogo público de una tienda del marketplace
 * (RF-18). Solo datos que el cliente puede ver; nada privado del negocio. {@code etiquetas} son las
 * etiquetas de la prenda ya resueltas a nombre (tipo + valor), para que el cliente sepa qué compra.
 */
public record PrendaEnVitrina(UUID id, String nombre, String tipoArticulo, BigDecimal precioRenta,
		BigDecimal precioVenta, String categoria, String fotoUrl, List<EtiquetaEnVitrina> etiquetas) {
}
