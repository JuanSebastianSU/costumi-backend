package com.costumi.backend.ventas.adaptadores.entrada;

import com.costumi.backend.ventas.dominio.Venta;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** DTO de salida de la Venta con sus líneas. */
public record VentaResponse(UUID id, UUID sucursalId, UUID empleadoId, UUID clienteId, BigDecimal descuento,
		BigDecimal total, String estado, List<LineaResponse> lineas) {

	public record LineaResponse(UUID prendaId, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {
	}

	static VentaResponse desde(Venta v) {
		List<LineaResponse> lineas = v.lineas().stream()
				.map(l -> new LineaResponse(l.prendaId(), l.cantidad(), l.precioUnitario(), l.subtotal()))
				.toList();
		return new VentaResponse(v.id(), v.sucursalId(), v.empleadoId(), v.clienteId(), v.descuento(), v.total(),
				v.estado().name(), lineas);
	}
}
