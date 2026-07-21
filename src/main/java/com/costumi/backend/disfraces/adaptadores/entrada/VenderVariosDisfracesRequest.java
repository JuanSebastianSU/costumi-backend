package com.costumi.backend.disfraces.adaptadores.entrada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para vender VARIOS disfraces distintos al mismo cliente en una sola operación. Cada
 * {@code item} es un disfraz con su cantidad y las prendas elegidas por slot. Sin fechas: venta inmediata.
 */
public record VenderVariosDisfracesRequest(

		@NotNull(message = "La sucursal es obligatoria") UUID sucursalId,

		/** Tienda donde se vende. Requerido para el rol CLIENTE; el personal la toma del token. */
		UUID empresaId,

		/** Ficha de cliente (modo asistido del personal). El CLIENTE usa su propia ficha (por token). */
		UUID clienteId,

		@NotEmpty(message = "El pedido debe tener al menos un disfraz") @Valid List<ItemDisfrazDto> items) {

	/** Un disfraz del pedido: cuál, cuántas unidades (nulo/<1 = 1) y las prendas elegidas por slot. */
	public record ItemDisfrazDto(

			@NotNull(message = "El disfraz es obligatorio") UUID disfrazId,

			@Min(value = 1, message = "La cantidad debe ser mayor o igual a 1") Integer cantidad,

			@Valid List<SeleccionSlotDto> selecciones) {
	}

	/** Elección de prenda para un slot, por su número de orden. */
	public record SeleccionSlotDto(

			@Min(value = 1, message = "El orden del slot debe ser mayor o igual a 1") int orden,

			@NotNull(message = "La prenda elegida es obligatoria") UUID prendaId) {
	}
}
