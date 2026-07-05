package com.costumi.backend.rentas.aplicacion;

import java.util.UUID;

/** La Renta no existe o no pertenece a la empresa del usuario. */
public class RentaNoEncontrada extends RuntimeException {

	public RentaNoEncontrada(UUID id) {
		super("No existe la renta " + id + " en esta empresa");
	}
}
