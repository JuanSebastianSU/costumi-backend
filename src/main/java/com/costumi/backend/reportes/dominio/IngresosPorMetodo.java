package com.costumi.backend.reportes.dominio;

import java.math.BigDecimal;

/**
 * Corte de ingresos netos (cobros − reembolsos, sin contar depósitos) por método de pago (RF-6.10/9.1).
 * Modelo de lectura sobre la tabla {@code pago}.
 */
public record IngresosPorMetodo(BigDecimal efectivo, BigDecimal tarjeta, BigDecimal transferencia, BigDecimal total) {
}
