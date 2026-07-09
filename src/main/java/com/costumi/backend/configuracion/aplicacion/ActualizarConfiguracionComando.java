package com.costumi.backend.configuracion.aplicacion;

import com.costumi.backend.configuracion.dominio.RecargoPorRetraso;

import java.math.BigDecimal;
import java.util.UUID;

/** Datos para actualizar interruptores, impuesto y reglas por defecto (moneda, recargo) de una empresa (RF-12.2/12.4). */
public record ActualizarConfiguracionComando(UUID empresaId, boolean conteoStock, boolean multasActivo,
		boolean multiSucursal, boolean pagoEnLinea, BigDecimal tasaImpuesto, String moneda,
		BigDecimal recargoPorRetrasoPorDia, RecargoPorRetraso modoRecargoRetraso) {
}
