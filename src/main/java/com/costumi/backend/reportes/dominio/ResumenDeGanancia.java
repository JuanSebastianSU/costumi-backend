package com.costumi.backend.reportes.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Resumen de ganancia (RF-9): ingresos totales, costo de lo vendido y la ganancia (ingreso − costo).
 * Modelo de lectura; sin costo de adquisición en las prendas no hay margen (RF-2.10).
 */
public record ResumenDeGanancia(BigDecimal ingresos, BigDecimal costoDeVentas, BigDecimal ganancia) {

	public static ResumenDeGanancia de(BigDecimal ingresos, BigDecimal costoDeVentas) {
		BigDecimal ing = Objects.requireNonNullElse(ingresos, BigDecimal.ZERO);
		BigDecimal costo = Objects.requireNonNullElse(costoDeVentas, BigDecimal.ZERO);
		return new ResumenDeGanancia(ing, costo, ing.subtract(costo));
	}
}
