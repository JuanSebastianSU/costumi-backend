package com.costumi.backend.clientes.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Un ítem del historial de un cliente (RF-7.2): una operación suya (RENTA o VENTA) con su monto,
 * estado y fecha (la de retiro para rentas; las ventas no llevan fecha propia). Modelo de lectura.
 */
public record HistorialItem(String tipo, UUID operacionId, BigDecimal monto, String estado, LocalDate fecha) {
}
