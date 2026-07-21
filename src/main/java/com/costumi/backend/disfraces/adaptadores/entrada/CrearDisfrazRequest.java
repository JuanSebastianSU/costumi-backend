package com.costumi.backend.disfraces.adaptadores.entrada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para crear/editar un Disfraz: su nombre, su {@code categoriaId} (opcional, para
 * agruparlo/verlo por categoría), la lista de {@code slots} (1..8, fijos o personalizables) y, opcional,
 * un {@code precioRentaGeneral} por día que anula la suma por prendas.
 */
public record CrearDisfrazRequest(

		@NotBlank(message = "El nombre del disfraz es obligatorio")
		String nombre,

		UUID categoriaId,

		BigDecimal precioRentaGeneral,

		BigDecimal precioVentaGeneral,

		@Valid
		List<SlotDto> slots) {

	public CrearDisfrazRequest {
		slots = (slots == null) ? List.of() : slots;
	}
}
