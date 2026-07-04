package com.costumi.backend.identidad.adaptadores.entrada;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** DTO de entrada del login. */
public record LoginRequest(

		@NotBlank(message = "El email es obligatorio")
		@Email(message = "El email no es válido")
		String email,

		@NotBlank(message = "La contraseña es obligatoria")
		String password) {
}
