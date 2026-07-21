package com.costumi.backend.disfraces.aplicacion;

import java.util.List;
import java.util.UUID;

/**
 * Datos para vender VARIOS disfraces (distintos, con cantidad) al mismo cliente en una sola venta. Cada
 * {@code item} resuelve un disfraz a sus piezas; todas las líneas se acumulan en una única venta.
 */
public record VenderVariosDisfracesComando(UUID empresaId, UUID sucursalId, UUID clienteId, List<ItemDeDisfraz> items,
		UUID empleadoId) {

	/** Un disfraz del pedido: cuál, cuántas unidades y las prendas elegidas por slot. */
	public record ItemDeDisfraz(UUID disfrazId, int cantidad, List<SeleccionDeSlot> selecciones) {
	}

	/** Elección del cliente para un slot: su número de orden y la prenda elegida (para personalizables). */
	public record SeleccionDeSlot(int orden, UUID prendaId) {
	}
}
