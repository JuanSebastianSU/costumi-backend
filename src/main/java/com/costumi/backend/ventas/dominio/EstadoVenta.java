package com.costumi.backend.ventas.dominio;

/**
 * Estado de una venta (RF-4.5). Se confirma al registrarse; puede devolverse en partes
 * ({@code PARCIALMENTE_DEVUELTA}) y queda {@code DEVUELTA} cuando se devuelven todas sus unidades.
 */
public enum EstadoVenta {

	CONFIRMADA,
	PARCIALMENTE_DEVUELTA,
	DEVUELTA
}
