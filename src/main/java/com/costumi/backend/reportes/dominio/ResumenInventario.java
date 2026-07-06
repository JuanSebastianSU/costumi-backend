package com.costumi.backend.reportes.dominio;

import java.math.BigDecimal;

/**
 * Resumen agregado de inventario (RF-9.1): unidades por estado, unidades rentadas ahora, tasa de
 * utilización (rentadas ahora / total) y valor de inventario (disponibles × precio de venta).
 */
public record ResumenInventario(long totalUnidades, long disponibles, long danadas, long enLimpieza, long perdidas,
		long rentadasAhora, BigDecimal tasaUtilizacion, BigDecimal valorInventario) {
}
