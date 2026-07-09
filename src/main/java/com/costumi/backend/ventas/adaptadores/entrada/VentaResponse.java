package com.costumi.backend.ventas.adaptadores.entrada;

import com.costumi.backend.ventas.dominio.LineaDeVenta;
import com.costumi.backend.ventas.dominio.Venta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/** DTO de salida de la Venta con sus líneas. {@code montoReembolsado} es el total ya devuelto (RF-4.5). */
public record VentaResponse(UUID id, UUID sucursalId, UUID empleadoId, UUID clienteId, BigDecimal descuento,
		BigDecimal total, String estado, BigDecimal montoReembolsado, List<LineaResponse> lineas) {

	public record LineaResponse(UUID prendaId, int cantidad, int cantidadDevuelta, BigDecimal precioUnitario,
			BigDecimal subtotal) {
	}

	static VentaResponse desde(Venta v) {
		List<LineaResponse> lineas = v.lineas().stream()
				.map(l -> new LineaResponse(l.prendaId(), l.cantidad(), l.cantidadDevuelta(), l.precioUnitario(),
						l.subtotal()))
				.toList();
		return new VentaResponse(v.id(), v.sucursalId(), v.empleadoId(), v.clienteId(), v.descuento(), v.total(),
				v.estado().name(), montoReembolsado(v), lineas);
	}

	/** Dinero ya reembolsado: la porción del total que corresponde a las unidades devueltas (proporcional). */
	private static BigDecimal montoReembolsado(Venta v) {
		BigDecimal subtotalOriginal = v.lineas().stream().map(LineaDeVenta::subtotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		if (subtotalOriginal.signum() == 0) {
			return BigDecimal.ZERO;
		}
		BigDecimal devueltoSubtotal = v.lineas().stream().map(LineaDeVenta::subtotalDevuelto)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return v.total().multiply(devueltoSubtotal).divide(subtotalOriginal, 2, RoundingMode.HALF_UP);
	}
}
