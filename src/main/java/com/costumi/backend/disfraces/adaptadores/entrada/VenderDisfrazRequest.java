package com.costumi.backend.disfraces.adaptadores.entrada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para vender un disfraz. {@code selecciones} indica la prenda elegida por número de orden
 * del slot (personalizables y opcionales incluidos). Sin fechas: la venta es inmediata.
 */
public record VenderDisfrazRequest(

		@NotNull(message = "La sucursal es obligatoria") UUID sucursalId,

		/** Tienda donde se vende. Requerido para el rol CLIENTE; el personal la toma del token. */
		UUID empresaId,

		/** Ficha de cliente (modo asistido del personal). El CLIENTE usa su propia ficha (por token). */
		UUID clienteId,

		/** Cuántas unidades del disfraz se venden (para "N iguales" en una operación). Nulo o < 1 = 1. */
		@Min(value = 1, message = "La cantidad debe ser mayor o igual a 1") Integer cantidad,

		@Valid List<SeleccionSlotDto> selecciones) {

	/** Elección de prenda para un slot, por su número de orden. */
	public record SeleccionSlotDto(

			@Min(value = 1, message = "El orden del slot debe ser mayor o igual a 1") int orden,

			@NotNull(message = "La prenda elegida es obligatoria") UUID prendaId) {
	}
}
