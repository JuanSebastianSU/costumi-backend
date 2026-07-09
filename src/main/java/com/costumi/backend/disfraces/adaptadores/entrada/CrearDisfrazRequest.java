package com.costumi.backend.disfraces.adaptadores.entrada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * DTO de entrada para crear/editar un Disfraz: su nombre y la lista de {@code slots} (1..8, fijos o
 * personalizables). Un disfraz de una sola pieza es un único slot fijo.
 */
public record CrearDisfrazRequest(

		@NotBlank(message = "El nombre del disfraz es obligatorio")
		String nombre,

		@Valid
		List<SlotDto> slots) {

	public CrearDisfrazRequest {
		slots = (slots == null) ? List.of() : slots;
	}
}
