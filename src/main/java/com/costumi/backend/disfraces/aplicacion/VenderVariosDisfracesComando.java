package com.costumi.backend.disfraces.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Datos para vender un pedido al mismo cliente en una sola venta: {@code items} (disfraces, cada uno con
 * cantidad y sus piezas elegidas) y/o {@code lineas} (prendas sueltas). Todo se acumula en una única venta.
 */
public record VenderVariosDisfracesComando(UUID empresaId, UUID sucursalId, UUID clienteId, List<ItemDeDisfraz> items,
		List<LineaDePrenda> lineas, UUID empleadoId) {

	/** Un disfraz del pedido: cuál, cuántas unidades y las prendas elegidas por slot. */
	public record ItemDeDisfraz(UUID disfrazId, int cantidad, List<SeleccionDeSlot> selecciones) {
	}

	/** Una prenda suelta del pedido: cuál, cuántas y su precio unitario. */
	public record LineaDePrenda(UUID prendaId, int cantidad, BigDecimal precioUnitario) {
	}

	/** Elección del cliente para un slot: su número de orden y la prenda elegida (para personalizables). */
	public record SeleccionDeSlot(int orden, UUID prendaId) {
	}
}
