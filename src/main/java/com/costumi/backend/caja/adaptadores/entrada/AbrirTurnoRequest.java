package com.costumi.backend.caja.adaptadores.entrada;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

/** DTO de entrada para abrir un turno de caja. */
public record AbrirTurnoRequest(

		@NotNull(message = "La sucursal es obligatoria")
		UUID sucursalId,

		@NotNull(message = "El fondo inicial es obligatorio")
		@PositiveOrZero(message = "El fondo inicial no puede ser negativo")
		BigDecimal fondoInicial) {
}
