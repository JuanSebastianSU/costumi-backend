package com.costumi.backend.ventas.adaptadores.entrada;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** DTO de entrada para registrar una Venta. El empleado se toma del token, no del body. */
public record RegistrarVentaRequest(

		@NotNull(message = "La sucursal es obligatoria") UUID sucursalId,

		UUID clienteId,

		BigDecimal descuento,

		/** Opcional (RF-17.6): clave para no duplicar la venta ante reintentos/offline. */
		String claveIdempotencia,

		@NotEmpty(message = "La venta debe tener al menos una línea")
		@Valid List<LineaVentaRequest> lineas) {

	/** Una línea de la venta. */
	public record LineaVentaRequest(

			@NotNull(message = "La prenda es obligatoria") UUID prendaId,

			@Min(value = 1, message = "La cantidad debe ser mayor a 0") int cantidad,

			@NotNull(message = "El precio unitario es obligatorio")
			@Positive(message = "El precio unitario debe ser mayor a 0") BigDecimal precioUnitario) {
	}
}
