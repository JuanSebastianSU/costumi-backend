package com.costumi.backend.pagos.dominio;

import java.math.BigDecimal;

/**
 * Una porción de un cobro mixto (RF-6.7): parte del total pagada con un método concreto
 * (efectivo, tarjeta, transferencia) y, si aplica, su referencia/autorización.
 */
public record PorcionDePago(MetodoPago metodo, BigDecimal monto, String referencia) {

	public PorcionDePago {
		if (metodo == null) {
			throw new IllegalArgumentException("Cada porción del pago necesita un método");
		}
		if (monto == null || monto.signum() <= 0) {
			throw new IllegalArgumentException("El monto de cada porción debe ser mayor a 0");
		}
	}
}
