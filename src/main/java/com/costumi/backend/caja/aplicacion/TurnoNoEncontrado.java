package com.costumi.backend.caja.aplicacion;

import java.util.UUID;

/** El turno no existe o no pertenece a la empresa del usuario (no se revela cuál). Se traduce a 404. */
public class TurnoNoEncontrado extends RuntimeException {

	public TurnoNoEncontrado(UUID id) {
		super("No existe el turno de caja " + id + " en esta empresa");
	}
}
