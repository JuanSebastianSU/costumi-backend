package com.costumi.backend.catalogo.aplicacion;

import java.util.UUID;

/** El Tipo de etiqueta no existe o no pertenece a la empresa del usuario (no se revela cuál). */
public class TipoEtiquetaNoEncontrado extends RuntimeException {

	public TipoEtiquetaNoEncontrado(UUID id) {
		super("No existe el tipo de etiqueta " + id + " en esta empresa");
	}
}
