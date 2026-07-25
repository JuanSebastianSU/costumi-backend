package com.costumi.backend.ventas.adaptadores.entrada;

import com.costumi.backend.ventas.dominio.TotalesDeVentas;

import java.math.BigDecimal;

/** DTO de salida con los totales del período de ventas (G8). */
public record TotalesDeVentasResponse(long cantidad, BigDecimal total) {

	static TotalesDeVentasResponse desde(TotalesDeVentas t) {
		return new TotalesDeVentasResponse(t.cantidad(), t.total());
	}
}
