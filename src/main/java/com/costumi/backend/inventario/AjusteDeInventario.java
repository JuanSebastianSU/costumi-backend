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
	void descontarDisponibles(UUID empresaId, UUID sucursalId, UUID prendaId, int cantidad);

	/**
	 * Reingresa {@code cantidad} unidades disponibles de la prenda a su stock en la sucursal (p. ej. al
	 * devolver una venta, RF-4.5). Lanza {@link StockInsuficiente} si la prenda no tiene grupo de stock
	 * en esa sucursal donde reingresarlas.
	 */
	void reingresarDisponibles(UUID empresaId, UUID sucursalId, UUID prendaId, int cantidad);

	/**
	 * Procesa el retorno de una renta según el checklist de la devolución (RF-5.4/5.6): mueve unidades
	 * de <b>disponible</b> a dañadas / en limpieza / perdidas. Las piezas que vuelven bien quedan
	 * disponibles (no se mueven). Lanza {@link StockInsuficiente} si no hay disponibles suficientes.
	 */
	void procesarRetornoDeRenta(UUID empresaId, UUID sucursalId, UUID prendaId, int danadas, int enLimpieza,
			int perdidas);
}
