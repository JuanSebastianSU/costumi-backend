package com.costumi.backend.clientes.adaptadores.entrada;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO de entrada para dar de alta un Cliente. */
public record CrearClienteRequest(

		@NotBlank(message = "El nombre del cliente es obligatorio")
		@Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
		String nombre,

		@Size(max = 40, message = "El teléfono no puede exceder 40 caracteres")
		String telefono,

		@Email(message = "El correo no es válido")
		@Size(max = 200, message = "El correo no puede exceder 200 caracteres")
		String email,

		@Size(max = 60, message = "El documento no puede exceder 60 caracteres")
		String documento,

		@Size(max = 300, message = "La dirección no puede exceder 300 caracteres")
		String direccion) {
}
