package com.costumi.backend.reportes.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Fila del reporte de rentas vencidas (RF-9.1): una renta ACTIVA cuya fecha de devolución ya pasó.
 * Modelo de lectura sobre el esquema compartido.
 */
public record RentaVencida(UUID rentaId, UUID clienteId, UUID prendaId, LocalDate fechaDevolucion, BigDecimal importe,
		BigDecimal deposito) {
}
