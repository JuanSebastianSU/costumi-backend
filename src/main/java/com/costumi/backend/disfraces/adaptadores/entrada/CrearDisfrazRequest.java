package com.costumi.backend.disfraces.adaptadores.entrada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de entrada para crear/editar un Disfraz: su nombre, la lista de {@code slots} (1..8, fijos o
 * personalizables) y, opcional, un {@code precioRentaGeneral} por día que anula la suma por prendas.
 * Un disfraz de una sola pieza es un único slot fijo.
 */
public record CrearDisfrazRequest(

		@NotBlank(message = "El nombre del disfraz es obligatorio")
		String nombre,

		BigDecimal precioRentaGeneral,

		@Valid
		List<SlotDto> slots) {

	public CrearDisfrazRequest {
		slots = (slots == null) ? List.of() : slots;
	}
}
