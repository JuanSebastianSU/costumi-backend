package com.costumi.backend.identidad.aplicacion;

import java.util.UUID;

/** No existe una sucursal con ese id en la empresa (tenant) — RF-15.1. */
public class SucursalNoEncontrada extends RuntimeException {

	public SucursalNoEncontrada(UUID sucursalId) {
		super("La sucursal " + sucursalId + " no existe en esta empresa");
	}
}
