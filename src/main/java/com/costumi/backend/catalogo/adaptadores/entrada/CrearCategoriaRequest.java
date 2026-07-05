package com.costumi.backend.catalogo.adaptadores.entrada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO de entrada para crear una Categoría. */
public record CrearCategoriaRequest(

		@NotBlank(message = "El nombre de la categoría es obligatorio")
		@Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
		String nombre) {
}
