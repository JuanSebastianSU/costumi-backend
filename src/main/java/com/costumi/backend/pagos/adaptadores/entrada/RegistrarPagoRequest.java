package com.costumi.backend.pagos.adaptadores.entrada;

import com.costumi.backend.pagos.dominio.MetodoPago;
import com.costumi.backend.pagos.dominio.TipoConcepto;
import com.costumi.backend.pagos.dominio.TipoPago;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/** DTO de entrada para registrar un Pago. {@code tipoPago} ausente = COBRO; REEMBOLSO devuelve dinero. */
public record RegistrarPagoRequest(

		@NotNull(message = "La sucursal es obligatoria") UUID sucursalId,

		@NotNull(message = "El tipo de concepto (RENTA/VENTA) es obligatorio") TipoConcepto tipoConcepto,

		@NotNull(message = "El concepto (renta/venta) es obligatorio") UUID conceptoId,

		@NotNull(message = "El monto es obligatorio")
		@Positive(message = "El monto debe ser mayor a 0") BigDecimal monto,

		TipoPago tipoPago,

		@NotNull(message = "El método de pago es obligatorio") MetodoPago metodo,

		String referencia,

		String claveIdempotencia) {
}
