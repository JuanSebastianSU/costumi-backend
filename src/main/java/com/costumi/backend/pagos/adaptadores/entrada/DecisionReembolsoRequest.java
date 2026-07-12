package com.costumi.backend.pagos.adaptadores.entrada;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DTO de entrada para aprobar o rechazar un reembolso: el motivo es obligatorio (RF-4.5/6.9). */
public record DecisionReembolsoRequest(

		@NotBlank(message = "El motivo de la decisión es obligatorio")
		@Size(max = 500, message = "El motivo no puede exceder 500 caracteres")
		String motivo) {
}
