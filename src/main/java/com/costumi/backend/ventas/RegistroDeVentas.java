package com.costumi.backend.ventas;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * API pública de Ventas para otros módulos (§5.5): permite registrar una venta desde ítems ya
 * resueltos (p. ej. el <b>checkout</b> de un carrito, RF-16) sin conocer las clases internas de Ventas.
 */
public interface RegistroDeVentas {

	/** Una línea de la venta ya resuelta: prenda, cantidad y precio unitario. */
	record ItemDeVenta(UUID prendaId, int cantidad, BigDecimal precioUnitario) {
	}

	/** Registra la venta (descuenta stock, RF-4.4) y devuelve su id. */
	UUID registrar(UUID empresaId, UUID sucursalId, UUID empleadoId, UUID clienteId, List<ItemDeVenta> items);
}
