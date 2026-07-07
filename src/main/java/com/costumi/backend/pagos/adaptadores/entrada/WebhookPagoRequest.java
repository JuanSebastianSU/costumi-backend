package com.costumi.backend.pagos.adaptadores.entrada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** DTO de entrada del webhook de la pasarela (RF-6.11): identifica el intento y el pago externo. */
public record WebhookPagoRequest(

		@NotNull(message = "El intento es obligatorio")
		UUID intentoId,

		@NotBlank(message = "El id del pago externo es obligatorio")
		String idPagoExterno) {
}
