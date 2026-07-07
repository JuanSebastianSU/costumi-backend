package com.costumi.backend.identidad.adaptadores.entrada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada del registro / solicitud de tienda de Empresa. {@code ubicacion} y {@code contacto}
 * son opcionales (los carga el cliente al pedir abrir su tienda desde el marketplace).
 */
public record RegistrarEmpresaRequest(

		@NotBlank(message = "El nombre de la empresa es obligatorio")
		@Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
		String nombre,

		@Size(max = 300, message = "La ubicación no puede exceder 300 caracteres")
		String ubicacion,

		@Size(max = 200, message = "El contacto no puede exceder 200 caracteres")
		String contacto) {
}
