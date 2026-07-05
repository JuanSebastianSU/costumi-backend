package com.costumi.backend.pedidos.adaptadores.entrada;

import com.costumi.backend.pedidos.dominio.Carrito;

import java.util.List;
import java.util.UUID;

/** DTO de salida del Carrito con sus líneas. */
public record CarritoResponse(UUID id, UUID sucursalId, UUID clienteId, String tipo, String estado,
		List<LineaResponse> lineas) {

	public record LineaResponse(UUID prendaId, int cantidad) {
	}

	static CarritoResponse desde(Carrito carrito) {
		List<LineaResponse> lineas = carrito.lineas().stream()
				.map(l -> new LineaResponse(l.prendaId(), l.cantidad()))
				.toList();
		return new CarritoResponse(carrito.id(), carrito.sucursalId(), carrito.clienteId(),
				carrito.tipo().name(), carrito.estado().name(), lineas);
	}
}
