package com.costumi.backend.configuracion.adaptadores.entrada;

import com.costumi.backend.configuracion.dominio.RecargoPorRetraso;

import java.math.BigDecimal;

/**
 * DTO de entrada para interruptores + reglas por defecto. {@code null} usa el valor por defecto
 * (impuesto/recargo 0, moneda COP, recargo ACUMULATIVA).
 */
public record ConfiguracionRequest(boolean conteoStock, boolean multasActivo, boolean multiSucursal,
		boolean pagoEnLinea, BigDecimal tasaImpuesto, String moneda, BigDecimal recargoPorRetrasoPorDia,
		RecargoPorRetraso modoRecargoRetraso) {
}
