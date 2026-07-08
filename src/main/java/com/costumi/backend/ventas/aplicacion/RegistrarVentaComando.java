package com.costumi.backend.ventas.aplicacion;

import com.costumi.backend.ventas.dominio.LineaDeVenta;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Datos para registrar una Venta (RF-4.1/4.2/4.3). El empleado sale del token.
 * {@code claveIdempotencia} es opcional (null): evita duplicar la venta ante reintentos (RF-17.6).
 */
public record RegistrarVentaComando(UUID empresaId, UUID sucursalId, UUID empleadoId, UUID clienteId,
		BigDecimal descuento, List<LineaDeVenta> lineas, String claveIdempotencia) {
}
