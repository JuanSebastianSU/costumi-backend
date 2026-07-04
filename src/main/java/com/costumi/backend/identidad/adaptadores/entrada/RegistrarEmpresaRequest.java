package com.costumi.backend.identidad.adaptadores.entrada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO de entrada del endpoint de registro de Empresa. */
public record RegistrarEmpresaRequest(

		@NotBlank(message = "El nombre de la empresa es obligatorio")
		@Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
		String nombre) {
}
