package com.costumi.backend.pedidos.aplicacion;

import com.costumi.backend.pedidos.dominio.Carrito;
import com.costumi.backend.pedidos.dominio.TipoPedido;

import java.util.UUID;

/** Puerto de entrada: consulta el carrito PENDIENTE de (cliente × sucursal × tipo). */
public interface ConsultarCarrito {

	Carrito pendiente(UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo);
}
