package com.costumi.backend.rentas.adaptadores.entrada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para crear una Renta. Acepta dos formas equivalentes de indicar los artículos:
 * <ul>
 *   <li><b>Multi-artículo:</b> {@code lineas} con una o más prendas y sus cantidades (RF-3.1/16.2).</li>
 *   <li><b>Un solo artículo (compat):</b> {@code prendaId} + {@code precioPorDia} (cantidad 1).</li>
 * </ul>
 * La normalización a líneas la hace el controller.
 */
public record CrearRentaRequest(

		@NotNull(message = "La sucursal es obligatoria") UUID sucursalId,

		@NotNull(message = "El cliente es obligatorio") UUID clienteId,

		@NotNull(message = "La fecha de retiro es obligatoria") LocalDate fechaRetiro,

		@NotNull(message = "La fecha de devolución es obligatoria") LocalDate fechaDevolucion,

		BigDecimal deposito,

		String claveIdempotencia,

		// Forma multi-artículo.
		@Valid List<LineaRentaDto> lineas,

		// Forma de un solo artículo (compatibilidad).
		UUID prendaId,

		@Positive(message = "El precio por día debe ser mayor a 0") BigDecimal precioPorDia) {
}
