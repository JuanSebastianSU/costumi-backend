package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.dominio.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** DTO de entrada para dar de alta un empleado (RF-8). */
public record AltaDeEmpleadoRequest(

		@NotBlank(message = "El correo es obligatorio")
		@Email(message = "El correo no es válido") String email,

		@NotBlank(message = "La contraseña es obligatoria") String password,

		@NotNull(message = "El rol es obligatorio") Rol rol) {
}
