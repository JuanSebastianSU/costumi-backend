package com.costumi.backend.clientes.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Un ítem del historial de un cliente (RF-7.2): una operación suya (RENTA o VENTA) con su monto,
 * estado y fecha (la de retiro para rentas; las ventas no llevan fecha propia). Incluye la tienda
 * ({@code empresaId}/{@code empresaNombre}) para "Mis Pedidos" del marketplace, que cruza tiendas
 * (RF-18.9) y para poder solicitar el reembolso de esa operación, y el detalle de artículos
 * ({@code lineas}, cada uno con nombre y foto) para mostrar QUÉ se rentó/compró. Modelo de lectura.
 */
public record HistorialItem(String tipo, UUID operacionId, BigDecimal monto, String estado, LocalDate fecha,
		UUID empresaId, String empresaNombre, List<LineaDeHistorial> lineas) {
}
