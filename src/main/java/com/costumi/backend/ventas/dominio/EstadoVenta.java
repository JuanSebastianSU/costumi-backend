package com.costumi.backend.ventas.dominio;

/** Estado de una venta. Se confirma al registrarse; DEVUELTA para cambios/reintegros (RF-4.5). */
public enum EstadoVenta {

	CONFIRMADA,
	DEVUELTA
}
