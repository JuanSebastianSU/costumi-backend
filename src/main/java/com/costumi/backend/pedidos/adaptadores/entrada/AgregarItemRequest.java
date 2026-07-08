package com.costumi.backend.pedidos.adaptadores.entrada;

import com.costumi.backend.pedidos.dominio.TipoPedido;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de entrada para agregar un ítem al carrito. En un carrito de RENTA se envían las fechas de
 * retiro/devolución del artículo (RF-18.6); en uno de VENTA se omiten.
 *
 * <p>{@code empresaId} y {@code clienteId} se resuelven por rol en el controller:
 * <ul>
 *   <li><b>Personal de la empresa</b> (modo asistido): {@code empresaId} sale del token y
 *       {@code clienteId} viene en el request.</li>
 *   <li><b>CLIENTE del marketplace</b>: {@code empresaId} es la tienda que compra (viene en el
 *       request) y {@code clienteId} se fuerza a su propio id del token (se ignora el del request).</li>
 * </ul>
 */
public record AgregarItemRequest(

		@NotNull(message = "La sucursal es obligatoria") UUID sucursalId,

		/** Tienda a la que se compra. Requerido para el rol CLIENTE; el personal lo toma del token. */
		UUID empresaId,

		/** Ficha de cliente (modo asistido del personal). El CLIENTE usa su propio id del token. */
		UUID clienteId,

		@NotNull(message = "El tipo (RENTA/VENTA) es obligatorio") TipoPedido tipo,

		@NotNull(message = "La prenda es obligatoria") UUID prendaId,

		@Min(value = 1, message = "La cantidad debe ser mayor a 0") int cantidad,

		LocalDate fechaRetiro,

		LocalDate fechaDevolucion) {
}
