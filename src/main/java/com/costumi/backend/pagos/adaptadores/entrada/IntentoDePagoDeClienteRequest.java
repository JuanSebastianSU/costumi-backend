package com.costumi.backend.pagos.adaptadores.entrada;

import com.costumi.backend.pagos.dominio.TipoConcepto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de entrada para que un CLIENTE del marketplace inicie el pago en línea de su propia venta o renta
 * (RF-6.11/14.4). A diferencia del intento del personal, el cliente indica la {@code empresaId} (la tienda);
 * su ficha y la propiedad de la operación se resuelven desde su token.
 */
public record IntentoDePagoDeClienteRequest(

		@NotNull(message = "La empresa (tienda) es obligatoria")
		UUID empresaId,

		@NotNull(message = "La sucursal es obligatoria")
		UUID sucursalId,

		@NotNull(message = "El tipo de concepto es obligatorio (VENTA o RENTA)")
		TipoConcepto tipoConcepto,

		@NotNull(message = "El concepto (venta o renta) es obligatorio")
		UUID conceptoId,

		@NotNull(message = "El monto es obligatorio")
		@Positive(message = "El monto debe ser positivo")
		BigDecimal monto,

		String moneda) {
}
