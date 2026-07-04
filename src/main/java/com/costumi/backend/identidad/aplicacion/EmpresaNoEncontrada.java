package com.costumi.backend.identidad.aplicacion;

import java.util.UUID;

/** No existe una Empresa con el id solicitado. */
public class EmpresaNoEncontrada extends RuntimeException {

	public EmpresaNoEncontrada(UUID id) {
		super("No existe la empresa con id " + id);
	}
}
