package com.costumi.backend.catalogo.adaptadores.entrada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO de entrada para agregar un Valor a un Tipo de etiqueta. */
public record AgregarValorRequest(

		@NotBlank(message = "El valor es obligatorio")
		@Size(max = 120, message = "El valor no puede exceder 120 caracteres")
		String valor) {
}
