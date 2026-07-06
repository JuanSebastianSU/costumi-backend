package com.costumi.backend.pagos.adaptadores.entrada;

import com.costumi.backend.pagos.dominio.MetodoPago;
import com.costumi.backend.pagos.dominio.TipoConcepto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO de entrada para un cobro mixto (RF-6.7): varias porciones con métodos distintos sobre una
 * misma renta/venta. {@code efectivoRecibido} es opcional; si viene, se calcula el vuelto.
 */
public record RegistrarCobroMixtoRequest(

		@NotNull(message = "La sucursal es obligatoria") UUID sucursalId,

		@NotNull(message = "El tipo de concepto (RENTA/VENTA) es obligatorio") TipoConcepto tipoConcepto,

		@NotNull(message = "El concepto (renta/venta) es obligatorio") UUID conceptoId,

		@NotEmpty(message = "El cobro necesita al menos una porción")
		@Valid List<Porcion> porciones,

		@PositiveOrZero(message = "El efectivo recibido no puede ser negativo") BigDecimal efectivoRecibido,

		String claveIdempotencia) {

	/** Una porción del cobro: cuánto y con qué método. */
	public record Porcion(

			@NotNull(message = "El método de pago es obligatorio") MetodoPago metodo,

			@NotNull(message = "El monto de la porción es obligatorio")
			@Positive(message = "El monto de la porción debe ser mayor a 0") BigDecimal monto,

			String referencia) {
	}
}
