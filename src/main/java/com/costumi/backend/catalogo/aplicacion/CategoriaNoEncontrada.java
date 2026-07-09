package com.costumi.backend.catalogo.aplicacion;

import java.util.UUID;

/** No existe una categoría con ese id en la empresa (tenant). */
public class CategoriaNoEncontrada extends RuntimeException {

	public CategoriaNoEncontrada(UUID categoriaId) {
		super("La categoría " + categoriaId + " no existe en esta empresa");
	}
}
