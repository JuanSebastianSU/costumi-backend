package com.costumi.backend.reportes.dominio;

import java.math.BigDecimal;
import java.util.UUID;

/** Fila de un ranking de artículos (más vendidos/rentados, RF-9.1): prenda, unidades y monto. */
public record ArticuloRanking(UUID prendaId, String nombre, long unidades, BigDecimal monto) {
}
