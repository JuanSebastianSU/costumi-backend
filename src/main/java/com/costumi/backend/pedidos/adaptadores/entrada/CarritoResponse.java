package com.costumi.backend.pedidos.adaptadores.entrada;

import com.costumi.backend.inventario.ConsultaDeInventario;
import com.costumi.backend.pedidos.aplicacion.CarritoValorizado;
import com.costumi.backend.pedidos.dominio.Carrito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO de salida del Carrito con sus líneas y precios. En los carritos de RENTA cada línea lleva su
 * periodo. Al consultar el carrito PENDIENTE se incluyen {@code precioUnitario}/{@code subtotal} por
 * línea y el {@code total}, para que el cliente vea cuánto pagará antes de confirmar (RF-18.5). Cada
 * línea trae además {@code nombre} y {@code fotoUrl} de la prenda para mostrar QUÉ se agregó, con imagen.
 */
public record CarritoResponse(UUID id, UUID sucursalId, UUID clienteId, String tipo, String estado,
		List<LineaDeCarritoResponse> lineas, BigDecimal total) {

	public record LineaDeCarritoResponse(UUID prendaId, String nombre, String fotoUrl, int cantidad,
			LocalDate fechaRetiro, LocalDate fechaDevolucion, BigDecimal precioUnitario, BigDecimal subtotal) {
	}

	/** Respuesta tras agregar un ítem: aún sin valorizar (el total se ve al consultar el carrito). */
	static CarritoResponse desde(Carrito carrito, Map<UUID, ConsultaDeInventario.ResumenDePrenda> resumen) {
		List<LineaDeCarritoResponse> lineas = carrito.lineas().stream()
				.map(l -> new LineaDeCarritoResponse(l.prendaId(), nombre(resumen, l.prendaId()),
						fotoUrl(resumen, l.prendaId()), l.cantidad(), l.fechaRetiro(), l.fechaDevolucion(), null, null))
				.toList();
		return new CarritoResponse(carrito.id(), carrito.sucursalId(), carrito.clienteId(),
				carrito.tipo().name(), carrito.estado().name(), lineas, null);
	}

	/** Respuesta del carrito pendiente: con precio por línea y total calculados por el backend. */
	static CarritoResponse desde(CarritoValorizado carrito, Map<UUID, ConsultaDeInventario.ResumenDePrenda> resumen) {
		List<LineaDeCarritoResponse> lineas = carrito.lineas().stream()
				.map(l -> new LineaDeCarritoResponse(l.prendaId(), nombre(resumen, l.prendaId()),
						fotoUrl(resumen, l.prendaId()), l.cantidad(), l.fechaRetiro(), l.fechaDevolucion(),
						l.precioUnitario(), l.subtotal()))
				.toList();
		return new CarritoResponse(carrito.id(), carrito.sucursalId(), carrito.clienteId(),
				carrito.tipo().name(), carrito.estado().name(), lineas, carrito.total());
	}

	private static String nombre(Map<UUID, ConsultaDeInventario.ResumenDePrenda> resumen, UUID prendaId) {
		ConsultaDeInventario.ResumenDePrenda r = resumen.get(prendaId);
		return r == null ? null : r.nombre();
	}

	private static String fotoUrl(Map<UUID, ConsultaDeInventario.ResumenDePrenda> resumen, UUID prendaId) {
		ConsultaDeInventario.ResumenDePrenda r = resumen.get(prendaId);
		return r == null ? null : r.fotoUrl();
	}
}
