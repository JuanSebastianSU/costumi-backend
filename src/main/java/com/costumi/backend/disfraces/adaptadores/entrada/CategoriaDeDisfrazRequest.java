package com.costumi.backend.disfraces.adaptadores.entrada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO de entrada para crear o renombrar una Categoría de disfraz (solo el nombre). */
public record CategoriaDeDisfrazRequest(

		@NotBlank(message = "El nombre de la categoría es obligatorio")
		@Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
		String nombre) {
}
