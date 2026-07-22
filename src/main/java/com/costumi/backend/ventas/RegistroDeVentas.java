package com.costumi.backend.ventas;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * API pública de Ventas para otros módulos (§5.5): permite registrar una venta desde ítems ya
 * resueltos (p. ej. el <b>checkout</b> de un carrito, RF-16) sin conocer las clases internas de Ventas.
 */
public interface RegistroDeVentas {

	/**
	 * Una línea de la venta ya resuelta: prenda, cantidad y precio unitario. Si la línea salió de armar
	 * un disfraz, {@code disfrazId}/{@code disfrazGrupo}/{@code disfrazCantidad} dicen cuál: así el
	 * disfraz no se pierde al cobrar. {@code disfrazGrupo} distingue dos instancias del mismo disfraz
	 * dentro de la misma venta (p. ej. con piezas distintas).
	 */
	record ItemDeVenta(UUID prendaId, int cantidad, BigDecimal precioUnitario, UUID disfrazId, UUID disfrazGrupo,
			Integer disfrazCantidad, String disfrazNombre) {

		/** Prenda suelta (sin disfraz de origen). */
		public ItemDeVenta(UUID prendaId, int cantidad, BigDecimal precioUnitario) {
			this(prendaId, cantidad, precioUnitario, null, null, null, null);
		}
	}

	/** Registra la venta (descuenta stock, RF-4.4) y devuelve su id. */
	UUID registrar(UUID empresaId, UUID sucursalId, UUID empleadoId, UUID clienteId, List<ItemDeVenta> items);
}
