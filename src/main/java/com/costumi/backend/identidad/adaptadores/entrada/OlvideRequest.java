package com.costumi.backend.identidad.adaptadores.entrada;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** DTO de entrada de "olvidé mi contraseña": solo el correo. */
public record OlvideRequest(

		@NotBlank(message = "El email es obligatorio")
		@Email(message = "El email no es válido")
		String email) {
}
