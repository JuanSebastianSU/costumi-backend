package com.costumi.backend.pedidos.aplicacion;

import com.costumi.backend.pedidos.dominio.EstadoCarrito;
import com.costumi.backend.pedidos.dominio.SeleccionDeSlot;
import com.costumi.backend.pedidos.dominio.TipoPedido;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Vista del carrito con los precios ya calculados por el backend (fuente de verdad), para que el
 * cliente vea cuánto va a pagar ANTES de confirmar. VENTA: precioUnitario × cantidad. RENTA:
 * precioPorDía × cantidad × días del periodo (el depósito se gestiona aparte en el pago). Una línea es
 * de una prenda ({@code prendaId}) o de un disfraz ({@code disfrazId} + {@code selecciones}); para el
 * disfraz el {@code precioUnitario} es el precio de UN disfraz (suma de sus piezas resueltas).
 */
public record CarritoValorizado(UUID id, UUID sucursalId, UUID clienteId, TipoPedido tipo, EstadoCarrito estado,
		List<LineaValorizada> lineas, BigDecimal total) {

	/**
	 * {@code id} identifica la línea dentro del carrito (es lo que se manda para quitarla).
	 * {@code motivoNoDisponible} explica por qué una línea no se pudo valorizar (por ejemplo, el dueño
	 * cambió el tipo del disfraz después de agregarlo). Una línea así se devuelve igual —con precio nulo—
	 * para que el cliente la vea y pueda quitarla; el checkout sigue rechazándola.
	 */
	public record LineaValorizada(UUID id, UUID prendaId, UUID disfrazId, List<SeleccionDeSlot> selecciones,
			int cantidad, LocalDate fechaRetiro, LocalDate fechaDevolucion, BigDecimal precioUnitario,
			BigDecimal subtotal, String motivoNoDisponible) {
	}
}
