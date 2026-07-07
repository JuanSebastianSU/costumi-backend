package com.costumi.backend.identidad.adaptadores.entrada;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO de entrada del auto-registro de un cliente (usuario final del marketplace). */
public record RegistroRequest(

		@NotBlank(message = "El email es obligatorio")
		@Email(message = "El email no es válido")
		String email,

		@NotBlank(message = "La contraseña es obligatoria")
		@Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
		String password) {
}
