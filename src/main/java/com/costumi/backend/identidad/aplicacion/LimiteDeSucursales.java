package com.costumi.backend.identidad.aplicacion;

import java.util.UUID;

/**
 * La empresa intenta abrir una segunda sucursal con el módulo multi-sucursal apagado (RF-12.4). El
 * interruptor de configuración controla de verdad el comportamiento: sin multi-sucursal, solo una.
 */
public class LimiteDeSucursales extends RuntimeException {

	public LimiteDeSucursales(UUID empresaId) {
		super("La empresa " + empresaId + " tiene el módulo multi-sucursal apagado: solo puede tener una sucursal");
	}
}
