package com.costumi.backend.reportes.adaptadores.entrada;

import com.costumi.backend.reportes.dominio.ResumenDeGanancia;

import java.math.BigDecimal;

/** DTO de salida del resumen de ganancia (RF-9). */
public record GananciaResponse(BigDecimal ingresos, BigDecimal costoDeVentas, BigDecimal ganancia) {

	static GananciaResponse desde(ResumenDeGanancia r) {
		return new GananciaResponse(r.ingresos(), r.costoDeVentas(), r.ganancia());
	}
}
