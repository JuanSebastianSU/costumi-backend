package com.costumi.backend.catalogo.adaptadores.entrada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO para renombrar un tipo de etiqueta o un valor (el nuevo texto). */
public record RenombrarRequest(

		@NotBlank(message = "El nuevo nombre es obligatorio")
		@Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
		String nombre) {
}
