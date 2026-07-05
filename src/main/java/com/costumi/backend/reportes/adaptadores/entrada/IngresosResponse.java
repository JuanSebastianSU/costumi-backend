package com.costumi.backend.reportes.adaptadores.entrada;

import com.costumi.backend.reportes.dominio.ResumenDeIngresos;

import java.math.BigDecimal;

/** DTO de salida del resumen de ingresos. */
public record IngresosResponse(BigDecimal ingresosPorRenta, BigDecimal ingresosPorVenta, BigDecimal total) {

	static IngresosResponse desde(ResumenDeIngresos r) {
		return new IngresosResponse(r.ingresosPorRenta(), r.ingresosPorVenta(), r.total());
	}
}
