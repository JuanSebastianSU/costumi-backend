package com.costumi.backend.identidad.aplicacion;

import java.util.UUID;

/** El usuario autenticado intenta operar sobre una empresa (tenant) que no es la suya. */
public class AccesoAlTenantDenegado extends RuntimeException {

	public AccesoAlTenantDenegado(UUID empresaId) {
		super("No tiene permiso para operar sobre la empresa " + empresaId);
	}
}
