package com.costumi.backend.pedidos.adaptadores.entrada;

import com.costumi.backend.pedidos.aplicacion.CarritoValorizado;
import com.costumi.backend.pedidos.dominio.Carrito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO de salida del Carrito con sus líneas y precios. En los carritos de RENTA cada línea lleva su
 * periodo. Al consultar el carrito PENDIENTE se incluyen {@code precioUnitario}/{@code subtotal} por
 * línea y el {@code total}, para que el cliente vea cuánto pagará antes de confirmar (RF-18.5).
 */
public record CarritoResponse(UUID id, UUID sucursalId, UUID clienteId, String tipo, String estado,
		List<LineaDeCarritoResponse> lineas, BigDecimal total) {

	public record LineaDeCarritoResponse(UUID prendaId, int cantidad, LocalDate fechaRetiro, LocalDate fechaDevolucion,
			BigDecimal precioUnitario, BigDecimal subtotal) {
	}

	/** Respuesta tras agregar un ítem: aún sin valorizar (el total se ve al consultar el carrito). */
	static CarritoResponse desde(Carrito carrito) {
		List<LineaDeCarritoResponse> lineas = carrito.lineas().stream()
				.map(l -> new LineaDeCarritoResponse(l.prendaId(), l.cantidad(), l.fechaRetiro(), l.fechaDevolucion(),
						null, null))
				.toList();
		return new CarritoResponse(carrito.id(), carrito.sucursalId(), carrito.clienteId(),
				carrito.tipo().name(), carrito.estado().name(), lineas, null);
	}

	/** Respuesta del carrito pendiente: con precio por línea y total calculados por el backend. */
	static CarritoResponse desde(CarritoValorizado carrito) {
		List<LineaDeCarritoResponse> lineas = carrito.lineas().stream()
				.map(l -> new LineaDeCarritoResponse(l.prendaId(), l.cantidad(), l.fechaRetiro(), l.fechaDevolucion(),
						l.precioUnitario(), l.subtotal()))
				.toList();
		return new CarritoResponse(carrito.id(), carrito.sucursalId(), carrito.clienteId(),
				carrito.tipo().name(), carrito.estado().name(), lineas, carrito.total());
	}
}
