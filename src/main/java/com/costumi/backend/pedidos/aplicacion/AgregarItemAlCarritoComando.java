package com.costumi.backend.pedidos.aplicacion;

import com.costumi.backend.pedidos.dominio.SeleccionDeSlot;
import com.costumi.backend.pedidos.dominio.TipoPedido;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Datos para agregar un ítem al carrito pendiente de (empresa × sucursal × cliente × tipo). El ítem es
 * una PRENDA ({@code prendaId}) o un DISFRAZ ({@code disfrazId} + {@code selecciones} de prenda por
 * slot), exactamente uno. En los carritos de RENTA, {@code fechaRetiro}/{@code fechaDevolucion} son el
 * periodo del artículo (RF-18.6); en los de VENTA son nulas.
 */
public record AgregarItemAlCarritoComando(UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo,
		UUID prendaId, UUID disfrazId, List<SeleccionDeSlot> selecciones, int cantidad, LocalDate fechaRetiro,
		LocalDate fechaDevolucion) {

	/** ¿El ítem es un disfraz? (si no, es una prenda). */
	public boolean esDisfraz() {
		return disfrazId != null;
	}
}
