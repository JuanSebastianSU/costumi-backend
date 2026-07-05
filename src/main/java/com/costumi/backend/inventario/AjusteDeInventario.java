package com.costumi.backend.inventario;

import java.util.UUID;

/**
 * API pública de <b>escritura</b> del inventario para otros módulos (§5.5): permite descontar stock
 * disponible al confirmar operaciones (p. ej. una venta, RF-4.4) sin conocer las clases internas.
 */
public interface AjusteDeInventario {

	/**
	 * Descuenta {@code cantidad} unidades disponibles de la prenda (reparte entre sus grupos de stock).
	 * Lanza {@link StockInsuficiente} si no hay suficientes unidades disponibles.
	 */
	void descontarDisponibles(UUID empresaId, UUID prendaId, int cantidad);
}
