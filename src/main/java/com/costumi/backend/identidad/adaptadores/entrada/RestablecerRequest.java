package com.costumi.backend.identidad.adaptadores.entrada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO de entrada para restablecer la contraseña con el token recibido por email. */
public record RestablecerRequest(

		@NotBlank(message = "El token es obligatorio")
		String token,

		@NotBlank(message = "La contraseña es obligatoria")
		@Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
		String password) {
}
