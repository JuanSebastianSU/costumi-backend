package com.costumi.backend.disfraces.adaptadores.entrada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para rentar un pedido al mismo cliente en una sola renta: {@code items} (disfraces) y/o
 * {@code lineas} (prendas sueltas). Debe traer al menos uno de los dos (lo valida el controller).
 */
public record RentarVariosDisfracesRequest(

		@NotNull(message = "La sucursal es obligatoria") UUID sucursalId,

		/** Tienda a la que se renta. Requerido para el rol CLIENTE; el personal la toma del token. */
		UUID empresaId,

		/** Ficha de cliente (modo asistido del personal). El CLIENTE usa su propia ficha (por token). */
		UUID clienteId,

		@NotNull(message = "La fecha de retiro es obligatoria") LocalDate fechaRetiro,

		@NotNull(message = "La fecha de devolución es obligatoria") LocalDate fechaDevolucion,

		@Valid List<ItemDisfrazDto> items,

		@Valid List<LineaPrendaRentaDto> lineas) {

	/** Un disfraz del pedido: cuál, cuántas unidades (nulo/<1 = 1) y las prendas elegidas por slot. */
	public record ItemDisfrazDto(

			@NotNull(message = "El disfraz es obligatorio") UUID disfrazId,

			@Min(value = 1, message = "La cantidad debe ser mayor o igual a 1") Integer cantidad,

			@Valid List<SeleccionSlotDto> selecciones) {
	}

	/**
	 * Una prenda suelta del pedido: cuál, cuántas y su precio POR DÍA. Nombre propio (no {@code LineaPrendaDto})
	 * a propósito: springdoc colapsa records anidados homónimos en un solo schema, y este difiere del de venta
	 * ({@code precioPorDia} vs {@code precioUnitario}); con el mismo nombre el contrato mentía sobre este campo.
	 */
	public record LineaPrendaRentaDto(

			@NotNull(message = "La prenda es obligatoria") UUID prendaId,

			@Min(value = 1, message = "La cantidad debe ser mayor o igual a 1") Integer cantidad,

			@NotNull(message = "El precio por día es obligatorio") BigDecimal precioPorDia) {
	}

	/** Elección de prenda para un slot, por su número de orden. */
	public record SeleccionSlotDto(

			@Min(value = 1, message = "El orden del slot debe ser mayor o igual a 1") int orden,

			@NotNull(message = "La prenda elegida es obligatoria") UUID prendaId) {
	}
}
