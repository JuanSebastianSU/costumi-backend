package com.costumi.backend.configuracion.adaptadores.entrada;

import com.costumi.backend.configuracion.dominio.ConfiguracionDeEmpresa;

import java.math.BigDecimal;

/** DTO de salida de la configuración de la empresa. */
public record ConfiguracionResponse(boolean conteoStock, boolean multasActivo, boolean multiSucursal,
		boolean pagoEnLinea, BigDecimal tasaImpuesto) {

	static ConfiguracionResponse desde(ConfiguracionDeEmpresa c) {
		return new ConfiguracionResponse(c.conteoStock(), c.multasActivo(), c.multiSucursal(), c.pagoEnLinea(),
				c.tasaImpuesto());
	}
}
