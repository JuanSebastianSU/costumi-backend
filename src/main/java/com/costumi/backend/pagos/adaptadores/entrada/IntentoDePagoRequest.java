package com.costumi.backend.pagos.adaptadores.entrada;

import com.costumi.backend.pagos.dominio.TipoConcepto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/** DTO de entrada para iniciar un pago en línea (RF-6.11). */
public record IntentoDePagoRequest(

		@NotNull(message = "La sucursal es obligatoria")
		UUID sucursalId,

		@NotNull(message = "El tipo de concepto es obligatorio")
		TipoConcepto tipoConcepto,

		@NotNull(message = "El concepto es obligatorio")
		UUID conceptoId,

		@NotNull(message = "El monto es obligatorio")
		@Positive(message = "El monto debe ser positivo")
		BigDecimal monto,

		String moneda) {
}
