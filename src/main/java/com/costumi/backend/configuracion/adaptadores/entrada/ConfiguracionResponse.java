package com.costumi.backend.configuracion.adaptadores.entrada;

import com.costumi.backend.configuracion.dominio.ConfiguracionDeEmpresa;

/** DTO de salida de la configuración de la empresa. */
public record ConfiguracionResponse(boolean conteoStock, boolean multasActivo, boolean multiSucursal,
		boolean pagoEnLinea) {

	static ConfiguracionResponse desde(ConfiguracionDeEmpresa c) {
		return new ConfiguracionResponse(c.conteoStock(), c.multasActivo(), c.multiSucursal(), c.pagoEnLinea());
	}
}
