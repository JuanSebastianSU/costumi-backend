package com.costumi.backend.caja.adaptadores.entrada;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** DTO de entrada para cerrar un turno con el efectivo físico contado (arqueo, RF-6.10). */
public record CerrarTurnoRequest(

		@NotNull(message = "El efectivo contado es obligatorio")
		@PositiveOrZero(message = "El efectivo contado no puede ser negativo")
		BigDecimal efectivoContado) {
}
