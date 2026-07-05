package com.costumi.backend.catalogo.aplicacion;

import java.util.UUID;

/** El Valor de etiqueta no existe, no es del tipo indicado o no pertenece a la empresa del usuario. */
public class ValorEtiquetaNoEncontrado extends RuntimeException {

	public ValorEtiquetaNoEncontrado(UUID id) {
		super("No existe el valor de etiqueta " + id + " en esta empresa");
	}
}
