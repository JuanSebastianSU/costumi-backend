package com.costumi.backend.pedidos.adaptadores.entrada;

import com.costumi.backend.disfraces.ResolucionDeDisfraces;
import com.costumi.backend.inventario.ConsultaDeInventario;
import com.costumi.backend.pedidos.aplicacion.CarritoValorizado;
import com.costumi.backend.pedidos.dominio.Carrito;
import com.costumi.backend.pedidos.dominio.LineaDeCarrito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO de salida del Carrito con sus líneas y precios. Una línea es de una PRENDA ({@code prendaId}) o de
 * un DISFRAZ ({@code disfrazId} + {@code selecciones}). En los carritos de RENTA cada línea lleva su
 * periodo. Al consultar el carrito PENDIENTE se incluyen {@code precioUnitario}/{@code subtotal} por línea
 * y el {@code total}, para que el cliente vea cuánto pagará antes de confirmar (RF-18.5). Cada línea trae
 * {@code nombre} y {@code fotoUrl} (de la prenda o del disfraz) para mostrar QUÉ se agregó, con imagen.
 */
public record CarritoResponse(UUID id, UUID sucursalId, UUID clienteId, String tipo, String estado,
		List<LineaDeCarritoResponse> lineas, BigDecimal total) {

	/**
	 * {@code id} identifica la línea dentro del carrito: es lo que se manda a {@code DELETE
	 * /api/v1/carritos/items/{lineaId}} para quitarla. {@code motivoNoDisponible} viene con texto cuando
	 * esa línea ya no se puede valorizar (el dueño cambió el artículo después de que el cliente lo agregó);
	 * en ese caso {@code precioUnitario} y {@code subtotal} son nulos y la línea hay que quitarla.
	 */
	public record LineaDeCarritoResponse(UUID id, UUID prendaId, UUID disfrazId, String nombre, String fotoUrl,
			int cantidad, LocalDate fechaRetiro, LocalDate fechaDevolucion, BigDecimal precioUnitario,
			BigDecimal subtotal, String motivoNoDisponible, List<SeleccionResponse> selecciones) {
	}

	/** Elección de prenda por slot del disfraz (para reflejar QUÉ eligió el cliente). */
	public record SeleccionResponse(int orden, UUID prendaId) {
	}

	/** Respuesta tras agregar un ítem: aún sin valorizar (el total se ve al consultar el carrito). */
	static CarritoResponse desde(Carrito carrito, Map<UUID, ConsultaDeInventario.ResumenDePrenda> prendas,
			Map<UUID, ResolucionDeDisfraces.ResumenDeDisfraz> disfraces) {
		List<LineaDeCarritoResponse> lineas = carrito.lineas().stream()
				.map(l -> l.esDisfraz()
						? new LineaDeCarritoResponse(l.id(), null, l.disfrazId(),
								nombreDisfraz(disfraces, l.disfrazId()), fotoDisfraz(disfraces, l.disfrazId()),
								l.cantidad(), l.fechaRetiro(), l.fechaDevolucion(), null, null, null, selecciones(l))
						: new LineaDeCarritoResponse(l.id(), l.prendaId(), null, nombrePrenda(prendas, l.prendaId()),
								fotoPrenda(prendas, l.prendaId()), l.cantidad(), l.fechaRetiro(),
								l.fechaDevolucion(), null, null, null, List.of()))
				.toList();
		return new CarritoResponse(carrito.id(), carrito.sucursalId(), carrito.clienteId(),
				carrito.tipo().name(), carrito.estado().name(), lineas, null);
	}

	/** Respuesta del carrito pendiente: con precio por línea y total calculados por el backend. */
	static CarritoResponse desde(CarritoValorizado carrito, Map<UUID, ConsultaDeInventario.ResumenDePrenda> prendas,
			Map<UUID, ResolucionDeDisfraces.ResumenDeDisfraz> disfraces) {
		List<LineaDeCarritoResponse> lineas = carrito.lineas().stream()
				.map(l -> l.disfrazId() != null
						? new LineaDeCarritoResponse(l.id(), null, l.disfrazId(),
								nombreDisfraz(disfraces, l.disfrazId()), fotoDisfraz(disfraces, l.disfrazId()),
								l.cantidad(), l.fechaRetiro(), l.fechaDevolucion(), l.precioUnitario(), l.subtotal(),
								l.motivoNoDisponible(), seleccionesValorizadas(l))
						: new LineaDeCarritoResponse(l.id(), l.prendaId(), null, nombrePrenda(prendas, l.prendaId()),
								fotoPrenda(prendas, l.prendaId()), l.cantidad(), l.fechaRetiro(),
								l.fechaDevolucion(), l.precioUnitario(), l.subtotal(), l.motivoNoDisponible(),
								List.of()))
				.toList();
		return new CarritoResponse(carrito.id(), carrito.sucursalId(), carrito.clienteId(),
				carrito.tipo().name(), carrito.estado().name(), lineas, carrito.total());
	}

	private static List<SeleccionResponse> selecciones(LineaDeCarrito linea) {
		return linea.selecciones().stream().map(s -> new SeleccionResponse(s.orden(), s.prendaId())).toList();
	}

	private static List<SeleccionResponse> seleccionesValorizadas(CarritoValorizado.LineaValorizada linea) {
		return linea.selecciones().stream().map(s -> new SeleccionResponse(s.orden(), s.prendaId())).toList();
	}

	private static String nombrePrenda(Map<UUID, ConsultaDeInventario.ResumenDePrenda> resumen, UUID prendaId) {
		ConsultaDeInventario.ResumenDePrenda r = resumen.get(prendaId);
		return r == null ? null : r.nombre();
	}

	private static String fotoPrenda(Map<UUID, ConsultaDeInventario.ResumenDePrenda> resumen, UUID prendaId) {
		ConsultaDeInventario.ResumenDePrenda r = resumen.get(prendaId);
		return r == null ? null : r.fotoUrl();
	}

	private static String nombreDisfraz(Map<UUID, ResolucionDeDisfraces.ResumenDeDisfraz> resumen, UUID disfrazId) {
		ResolucionDeDisfraces.ResumenDeDisfraz r = resumen.get(disfrazId);
		return r == null ? null : r.nombre();
	}

	private static String fotoDisfraz(Map<UUID, ResolucionDeDisfraces.ResumenDeDisfraz> resumen, UUID disfrazId) {
		ResolucionDeDisfraces.ResumenDeDisfraz r = resumen.get(disfrazId);
		return r == null ? null : r.fotoUrl();
	}
}
