package com.costumi.backend.rentas.aplicacion;

import java.util.UUID;

/**
 * El cliente está en lista negra y no puede iniciar una nueva renta (RF-7.4). Se traduce a HTTP 409:
 * la solicitud es válida, pero el estado del cliente la impide.
 */
public class ClienteEnListaNegra extends RuntimeException {

	public ClienteEnListaNegra(UUID clienteId) {
		super("El cliente " + clienteId + " está en lista negra y no puede rentar");
	}
}
