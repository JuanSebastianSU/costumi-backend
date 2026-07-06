package com.costumi.backend.configuracion.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/** Datos para actualizar los interruptores de módulos y la tasa de impuesto de una empresa (RF-12.4/6.5). */
public record ActualizarConfiguracionComando(UUID empresaId, boolean conteoStock, boolean multasActivo,
		boolean multiSucursal, boolean pagoEnLinea, BigDecimal tasaImpuesto) {
}
