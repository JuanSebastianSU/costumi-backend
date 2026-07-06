package com.costumi.backend.reportes.dominio;

import java.math.BigDecimal;
import java.util.UUID;

/** Fila del reporte de ventas por empleado (RF-9.1): empleado, número de ventas y total vendido. */
public record EmpleadoVentas(UUID empleadoId, String email, long numeroVentas, BigDecimal total) {
}
