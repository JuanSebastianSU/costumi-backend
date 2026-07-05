package com.costumi.backend.reportes.dominio;

import java.math.BigDecimal;

/** Modelo de lectura: ingresos por renta y por venta de una empresa (RF-9.1). */
public class ResumenDeIngresos {

	private final BigDecimal ingresosPorRenta;
	private final BigDecimal ingresosPorVenta;

	private ResumenDeIngresos(BigDecimal ingresosPorRenta, BigDecimal ingresosPorVenta) {
		this.ingresosPorRenta = (ingresosPorRenta == null) ? BigDecimal.ZERO : ingresosPorRenta;
		this.ingresosPorVenta = (ingresosPorVenta == null) ? BigDecimal.ZERO : ingresosPorVenta;
	}

	public static ResumenDeIngresos de(BigDecimal ingresosPorRenta, BigDecimal ingresosPorVenta) {
		return new ResumenDeIngresos(ingresosPorRenta, ingresosPorVenta);
	}

	public BigDecimal total() {
		return ingresosPorRenta.add(ingresosPorVenta);
	}

	public BigDecimal ingresosPorRenta() {
		return ingresosPorRenta;
	}

	public BigDecimal ingresosPorVenta() {
		return ingresosPorVenta;
	}
}
