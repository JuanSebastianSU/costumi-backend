package com.costumi.backend.reportes.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Ingreso total de un día (para la serie/tendencia del panel, RF-9.1). */
public record IngresoDelDia(LocalDate fecha, BigDecimal monto) {
}
