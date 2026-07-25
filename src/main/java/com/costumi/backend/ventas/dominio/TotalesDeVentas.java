package com.costumi.backend.ventas.dominio;

import java.math.BigDecimal;

/** Totales de un conjunto de ventas (para el resumen del período de G8): cuántas y la suma de sus totales. */
public record TotalesDeVentas(long cantidad, BigDecimal total) {
}
