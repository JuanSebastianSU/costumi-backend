package com.costumi.backend.pedidos.adaptadores.entrada;

import com.costumi.backend.pedidos.dominio.CarritoAbierto;
import com.costumi.backend.pedidos.dominio.TipoPedido;

import java.util.UUID;

/**
 * DTO de salida de un carrito abierto: lo justo para pintar la lista y poder volver a él (los tres datos
 * que {@code GET /carritos} exige: empresa, sucursal y tipo).
 */
public record CarritoAbiertoResponse(UUID empresaId, String empresaNombre, UUID sucursalId,
		String sucursalNombre, TipoPedido tipo, int articulos) {

	static CarritoAbiertoResponse desde(CarritoAbierto c) {
		return new CarritoAbiertoResponse(c.empresaId(), c.empresaNombre(), c.sucursalId(), c.sucursalNombre(),
				c.tipo(), c.articulos());
	}
}
