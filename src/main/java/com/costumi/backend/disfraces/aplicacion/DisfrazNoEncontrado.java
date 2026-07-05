package com.costumi.backend.disfraces.aplicacion;

import java.util.UUID;

/** El disfraz no existe o no pertenece a la empresa del usuario (no se revela cuál). */
public class DisfrazNoEncontrado extends RuntimeException {

	public DisfrazNoEncontrado(UUID id) {
		super("No existe el disfraz " + id + " en esta empresa");
	}
}
