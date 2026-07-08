package com.costumi.backend.disfraces.adaptadores.entrada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para rentar un disfraz. {@code selecciones} indica la prenda elegida por número de
 * orden del slot (para los personalizables y los opcionales que se incluyen); puede omitirse en un
 * disfraz de unidad fija o si todos los slots son fijos obligatorios.
 */
public record RentarDisfrazRequest(

		@NotNull(message = "La sucursal es obligatoria") UUID sucursalId,

		@NotNull(message = "El cliente es obligatorio") UUID clienteId,

		@NotNull(message = "La fecha de retiro es obligatoria") LocalDate fechaRetiro,

		@NotNull(message = "La fecha de devolución es obligatoria") LocalDate fechaDevolucion,

		@Valid List<SeleccionSlotDto> selecciones) {

	/** Elección de prenda para un slot, por su número de orden. */
	public record SeleccionSlotDto(

			@Min(value = 1, message = "El orden del slot debe ser mayor o igual a 1") int orden,

			@NotNull(message = "La prenda elegida es obligatoria") UUID prendaId) {
	}
}
