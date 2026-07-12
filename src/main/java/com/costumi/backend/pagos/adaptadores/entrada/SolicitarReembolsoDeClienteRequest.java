package com.costumi.backend.pagos.adaptadores.entrada;

import com.costumi.backend.pagos.dominio.TipoConcepto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/** DTO de entrada para que un CLIENTE solicite el reembolso de su propia venta o renta (RF-4.5/14.4). */
public record SolicitarReembolsoDeClienteRequest(

		@NotNull(message = "La empresa (tienda) es obligatoria")
		UUID empresaId,

		@NotNull(message = "El tipo de concepto es obligatorio (VENTA o RENTA)")
		TipoConcepto tipoConcepto,

		@NotNull(message = "El id del concepto (venta o renta) es obligatorio")
		UUID conceptoId,

		@NotNull(message = "El monto es obligatorio")
		@Positive(message = "El monto debe ser mayor a 0")
		BigDecimal monto,

		@NotBlank(message = "El motivo de la solicitud es obligatorio")
		@Size(max = 500, message = "El motivo no puede exceder 500 caracteres")
		String motivo) {
}
