package com.costumi.backend.configuracion.adaptadores.entrada;

import java.math.BigDecimal;

/** DTO de entrada para actualizar los interruptores de módulos y la tasa de impuesto ({@code null} = 0). */
public record ConfiguracionRequest(boolean conteoStock, boolean multasActivo, boolean multiSucursal,
		boolean pagoEnLinea, BigDecimal tasaImpuesto) {
}
