package com.costumi.backend.marketplace.dominio;

import java.util.UUID;

/** Faceta del marketplace: una categoría (de prenda) presente en el catálogo público de una tienda (RF-18.1). */
public record CategoriaEnVitrina(UUID id, String nombre) {
}
