package com.costumi.backend.ventas.aplicacion;

import com.costumi.backend.ventas.dominio.LineaDeVenta;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Datos para registrar una Venta (RF-4.1/4.2/4.3). El empleado sale del token. */
public record RegistrarVentaComando(UUID empresaId, UUID sucursalId, UUID empleadoId, UUID clienteId,
		BigDecimal descuento, List<LineaDeVenta> lineas) {
}
