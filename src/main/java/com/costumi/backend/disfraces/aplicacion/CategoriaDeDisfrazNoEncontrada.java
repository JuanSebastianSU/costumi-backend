package com.costumi.backend.disfraces.aplicacion;

import java.util.UUID;

/** La categoría de disfraz no existe en la empresa (o está fuera del tenant). */
public class CategoriaDeDisfrazNoEncontrada extends RuntimeException {

	public CategoriaDeDisfrazNoEncontrada(UUID categoriaId) {
		super("La categoría de disfraz " + categoriaId + " no existe en esta empresa");
	}
}
