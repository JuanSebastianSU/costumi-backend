package com.costumi.backend.pedidos.aplicacion;

import com.costumi.backend.pedidos.dominio.TipoPedido;

import java.util.UUID;

/** Puerto de entrada: consulta el carrito PENDIENTE de (cliente × sucursal × tipo), ya valorizado. */
public interface ConsultarCarrito {

	CarritoValorizado pendiente(UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo);
}
