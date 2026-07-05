package com.costumi.backend.pedidos.aplicacion;

import com.costumi.backend.pedidos.dominio.TipoPedido;

import java.util.UUID;

/** Datos para agregar un ítem al carrito pendiente de (empresa × sucursal × cliente × tipo). */
public record AgregarItemAlCarritoComando(UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo,
		UUID prendaId, int cantidad) {
}
