package com.costumi.backend.rentas.adaptadores.entrada;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** DTO de entrada para crear una Renta. */
public record CrearRentaRequest(

		@NotNull(message = "La sucursal es obligatoria") UUID sucursalId,

		@NotNull(message = "El cliente es obligatorio") UUID clienteId,

		@NotNull(message = "La prenda es obligatoria") UUID prendaId,

		@NotNull(message = "La fecha de retiro es obligatoria") LocalDate fechaRetiro,

		@NotNull(message = "La fecha de devolución es obligatoria") LocalDate fechaDevolucion,

		@NotNull(message = "El precio por día es obligatorio")
		@Positive(message = "El precio por día debe ser mayor a 0") BigDecimal precioPorDia,

		BigDecimal deposito,

		String claveIdempotencia) {
}
