package com.costumi.backend.pedidos.aplicacion;

import com.costumi.backend.pedidos.dominio.EstadoCarrito;
import com.costumi.backend.pedidos.dominio.TipoPedido;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Vista del carrito con los precios ya calculados por el backend (fuente de verdad), para que el
 * cliente vea cuánto va a pagar ANTES de confirmar. VENTA: precioUnitario × cantidad. RENTA:
 * precioPorDía × cantidad × días del periodo (el depósito se gestiona aparte en el pago).
 */
public record CarritoValorizado(UUID id, UUID sucursalId, UUID clienteId, TipoPedido tipo, EstadoCarrito estado,
		List<LineaValorizada> lineas, BigDecimal total) {

	public record LineaValorizada(UUID prendaId, int cantidad, LocalDate fechaRetiro, LocalDate fechaDevolucion,
			BigDecimal precioUnitario, BigDecimal subtotal) {
	}
}
