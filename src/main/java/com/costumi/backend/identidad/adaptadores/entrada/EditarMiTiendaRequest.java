package com.costumi.backend.identidad.adaptadores.entrada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para que el Dueño edite la identidad de su tienda (A7). El nombre es obligatorio; el resto
 * son opcionales (vacío = se limpia). El logo y la portada van por sus endpoints multipart aparte.
 */
public record EditarMiTiendaRequest(

		@NotBlank(message = "El nombre de la tienda es obligatorio")
		@Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
		String nombre,

		@Size(max = 1000, message = "La descripción no puede exceder 1000 caracteres")
		String descripcion,

		@Size(max = 120, message = "La ciudad no puede exceder 120 caracteres")
		String ciudad,

		@Size(max = 300, message = "La ubicación no puede exceder 300 caracteres")
		String ubicacion,

		@Size(max = 200, message = "El contacto no puede exceder 200 caracteres")
		String contacto) {
}
