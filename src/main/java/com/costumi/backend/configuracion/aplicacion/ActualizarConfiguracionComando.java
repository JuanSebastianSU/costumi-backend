package com.costumi.backend.configuracion.aplicacion;

import java.util.UUID;

/** Datos para actualizar los interruptores de módulos de una empresa (RF-12.4). */
public record ActualizarConfiguracionComando(UUID empresaId, boolean conteoStock, boolean multasActivo,
		boolean multiSucursal, boolean pagoEnLinea) {
}
