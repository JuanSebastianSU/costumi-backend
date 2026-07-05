package com.costumi.backend.configuracion.adaptadores.entrada;

/** DTO de entrada para actualizar los interruptores de módulos. */
public record ConfiguracionRequest(boolean conteoStock, boolean multasActivo, boolean multiSucursal,
		boolean pagoEnLinea) {
}
