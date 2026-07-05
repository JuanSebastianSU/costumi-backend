package com.costumi.backend.disfraces.adaptadores.entrada;

import com.costumi.backend.disfraces.dominio.ModoDeDisfraz;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para crear un Disfraz. Para {@code UNIDAD_FIJA} se usa {@code prendaFijaId}; para
 * {@code POR_PARTES}, la lista de {@code slots}.
 */
public record CrearDisfrazRequest(

		@NotBlank(message = "El nombre del disfraz es obligatorio")
		String nombre,

		@NotNull(message = "El modo del disfraz es obligatorio")
		ModoDeDisfraz modo,

		UUID prendaFijaId,

		@Valid
		List<SlotDto> slots) {

	public CrearDisfrazRequest {
		slots = (slots == null) ? List.of() : slots;
	}
}
