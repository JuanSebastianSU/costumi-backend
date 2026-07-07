package com.costumi.backend.clientes.adaptadores.entrada;

import jakarta.validation.constraints.NotBlank;

/** DTO de entrada para registrar el token de dispositivo (push FCM, RF-18.11). */
public record DeviceTokenRequest(

		@NotBlank(message = "El token de dispositivo es obligatorio")
		String deviceToken) {
}
