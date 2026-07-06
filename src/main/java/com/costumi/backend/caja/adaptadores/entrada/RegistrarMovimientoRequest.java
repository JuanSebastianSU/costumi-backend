package com.costumi.backend.caja.adaptadores.entrada;

import com.costumi.backend.caja.dominio.MetodoDePago;
import com.costumi.backend.caja.dominio.TipoMovimiento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** DTO de entrada para registrar un movimiento de caja. */
public record RegistrarMovimientoRequest(

		@NotNull(message = "El tipo de movimiento es obligatorio")
		TipoMovimiento tipo,

		@NotBlank(message = "El concepto es obligatorio")
		String concepto,

		@NotNull(message = "El monto es obligatorio")
		@Positive(message = "El monto debe ser mayor a 0")
		BigDecimal monto,

		@NotNull(message = "El método de pago es obligatorio")
		MetodoDePago metodo) {
}
