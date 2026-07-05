package com.costumi.backend.catalogo.adaptadores.entrada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO de entrada para crear un Tipo de etiqueta. */
public record CrearTipoEtiquetaRequest(

		@NotBlank(message = "El nombre del tipo de etiqueta es obligatorio")
		@Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
		String nombre,

		boolean defineVariante,

		boolean seleccionablePorCliente) {
}
