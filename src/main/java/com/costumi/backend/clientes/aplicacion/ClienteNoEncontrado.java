package com.costumi.backend.clientes.aplicacion;

import java.util.UUID;

/** El Cliente no existe o no pertenece a la empresa del usuario. */
public class ClienteNoEncontrado extends RuntimeException {

	public ClienteNoEncontrado(UUID id) {
		super("No existe el cliente " + id + " en esta empresa");
	}
}
