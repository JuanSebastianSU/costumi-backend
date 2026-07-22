package com.costumi.backend.pedidos.adaptadores.entrada;

import com.costumi.backend.pedidos.dominio.TipoPedido;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para agregar un ítem al carrito. El ítem es una PRENDA ({@code prendaId}) o un DISFRAZ
 * ({@code disfrazId} + {@code selecciones} de prenda por slot personalizable), exactamente uno (lo valida
 * el controller). En un carrito de RENTA se envían las fechas de retiro/devolución del artículo (RF-18.6);
 * en uno de VENTA se omiten.
 */
public record AgregarItemRequest(

		@NotNull(message = "La sucursal es obligatoria") UUID sucursalId,

		/** Tienda a la que se compra. Requerido para el rol CLIENTE; el personal la toma del token. */
		UUID empresaId,

		/** Ficha de cliente (modo asistido del personal). El CLIENTE usa su propia ficha (por token). */
		UUID clienteId,

		@NotNull(message = "El tipo (RENTA/VENTA) es obligatorio") TipoPedido tipo,

		/** Prenda a agregar (si el ítem es una prenda). Excluyente con {@code disfrazId}. */
		UUID prendaId,

		/** Disfraz a agregar (si el ítem es un disfraz). Excluyente con {@code prendaId}. */
		UUID disfrazId,

		/** Elección de prenda por slot personalizable del disfraz (RF-2.3). Solo aplica con {@code disfrazId}. */
		List<SeleccionSlotDto> selecciones,

		@Min(value = 1, message = "La cantidad debe ser mayor a 0") int cantidad,

		LocalDate fechaRetiro,

		LocalDate fechaDevolucion) {

	/** Elección de prenda para un slot personalizable, por su número de orden. */
	public record SeleccionSlotDto(@NotNull Integer orden, @NotNull UUID prendaId) {
	}
}
