package com.costumi.backend.pedidos.dominio;

/** Estado del carrito/pedido. Segmentación: solo hay uno PENDIENTE por (cliente×sucursal×tipo). */
public enum EstadoCarrito {

	PENDIENTE,
	CONFIRMADO
}
