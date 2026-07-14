package com.costumi.backend.notificaciones.adaptadores.entrada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO de entrada para personalizar una plantilla: texto (obligatorio) y switch on/off. */
public record ActualizarPlantillaRequest(

		@NotBlank(message = "El texto de la plantilla es obligatorio")
		@Size(max = 1000, message = "El texto no puede exceder 1000 caracteres")
		String texto,

		boolean activa) {
}
