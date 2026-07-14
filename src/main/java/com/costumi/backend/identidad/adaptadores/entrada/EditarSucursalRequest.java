package com.costumi.backend.identidad.adaptadores.entrada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO de entrada para editar una Sucursal (RF-15.1). */
public record EditarSucursalRequest(

		@NotBlank(message = "El nombre de la sucursal es obligatorio")
		@Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
		String nombre,

		@Size(max = 300, message = "La dirección no puede exceder 300 caracteres")
		String direccion,

		@Size(max = 500, message = "El enlace de ubicación no puede exceder 500 caracteres")
		String ubicacionMaps) {
}
