package com.costumi.backend.pedidos.aplicacion;

import java.util.UUID;

/** Puerto de entrada: checkout del carrito de VENTA → crea la venta y confirma el carrito (RF-16). */
public interface HacerCheckout {

	/** Devuelve el id de la venta creada. */
	UUID ejecutar(UUID empresaId, UUID sucursalId, UUID clienteId, UUID empleadoId);
}
