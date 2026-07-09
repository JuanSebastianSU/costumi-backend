package com.costumi.backend.configuracion.adaptadores.entrada;

import com.costumi.backend.configuracion.dominio.ConfiguracionDeEmpresa;
import com.costumi.backend.configuracion.dominio.RecargoPorRetraso;

import java.math.BigDecimal;

/** DTO de salida de la configuración de la empresa. Sirve también de respaldo (RF-12.3). */
public record ConfiguracionResponse(boolean conteoStock, boolean multasActivo, boolean multiSucursal,
		boolean pagoEnLinea, BigDecimal tasaImpuesto, String moneda, BigDecimal recargoPorRetrasoPorDia,
		RecargoPorRetraso modoRecargoRetraso, boolean reembolsosActivos, int ventanaReembolsoDias) {

	static ConfiguracionResponse desde(ConfiguracionDeEmpresa c) {
		return new ConfiguracionResponse(c.conteoStock(), c.multasActivo(), c.multiSucursal(), c.pagoEnLinea(),
				c.tasaImpuesto(), c.moneda(), c.recargoPorRetrasoPorDia(), c.modoRecargoRetraso(),
				c.reembolsosActivos(), c.ventanaReembolsoDias());
	}
}
