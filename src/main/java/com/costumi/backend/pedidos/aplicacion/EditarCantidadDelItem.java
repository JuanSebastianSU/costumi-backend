package com.costumi.backend.pedidos.aplicacion;

import com.costumi.backend.pedidos.dominio.Carrito;
import com.costumi.backend.pedidos.dominio.TipoPedido;

import java.util.UUID;

/** Puerto de entrada: fija la cantidad de una línea del carrito pendiente (RF-16). Devuelve el carrito. */
public interface EditarCantidadDelItem {

	Carrito ejecutar(UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo, UUID lineaId, int cantidad);
}
