package com.costumi.backend.clientes.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Un ítem del historial de un cliente (RF-7.2): una operación suya (RENTA o VENTA) con su monto,
 * estado y fecha (cuándo se registró la operación). Incluye la tienda ({@code empresaId}/
 * {@code empresaNombre}) para "Mis Pedidos" del marketplace, que cruza tiendas (RF-18.9) y para poder
 * solicitar el reembolso de esa operación, y el detalle de artículos ({@code lineas}, cada uno con
 * nombre y foto) para mostrar QUÉ se rentó/compró.
 *
 * <p>{@code saldoPendiente} = lo que aún debe por esta operación (renta: importe + multa − cobrado neto;
 * venta: total − cobrado neto; 0 una vez cerrada/cancelada/devuelta). {@code estadoPago} lo resume:
 * {@code PAGADO} (saldo 0), {@code PARCIAL} (algo cobrado pero falta) o {@code PENDIENTE} (nada cobrado).
 * Modelo de lectura.
 */
public record HistorialItem(String tipo, UUID operacionId, String codigoRetiro, BigDecimal monto, String estado,
		BigDecimal saldoPendiente, String estadoPago, LocalDate fecha, UUID empresaId, String empresaNombre,
		List<LineaDeHistorial> lineas) {
}
