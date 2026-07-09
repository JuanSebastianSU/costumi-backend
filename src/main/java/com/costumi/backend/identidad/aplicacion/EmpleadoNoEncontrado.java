package com.costumi.backend.identidad.aplicacion;

import java.util.UUID;

/** No existe un empleado con ese id en la empresa (tenant) — RF-8. */
public class EmpleadoNoEncontrado extends RuntimeException {

	public EmpleadoNoEncontrado(UUID usuarioId) {
		super("El empleado " + usuarioId + " no existe en esta empresa");
	}
}
