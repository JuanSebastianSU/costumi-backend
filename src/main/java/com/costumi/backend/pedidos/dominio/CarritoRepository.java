package com.costumi.backend.pedidos.dominio;

import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia del Carrito con sus líneas (scoped por tenant). */
public interface CarritoRepository {

	Carrito guardar(Carrito carrito);

	Optional<Carrito> buscarPorId(UUID id);

	/** El carrito PENDIENTE de un cliente en una sucursal para un tipo (RF-16.2/16.3). */
	Optional<Carrito> buscarPendiente(UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo);

	/**
	 * Serializa el checkout de un pedido dentro de la transacción (RF-17.6): dos confirmaciones
	 * simultáneas del mismo (empresa × sucursal × cliente × tipo) no corren a la vez, evitando crear
	 * ventas/rentas duplicadas por doble submit.
	 */
	void bloquearPedido(UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo);
}
