package com.costumi.backend.inventario.aplicacion;

import java.util.UUID;

/** La Prenda no existe o no pertenece a la empresa del usuario. */
public class PrendaNoEncontrada extends RuntimeException {

	public PrendaNoEncontrada(UUID id) {
		super("No existe la prenda " + id + " en esta empresa");
	}
}
