package com.costumi.backend.identidad.aplicacion;

/** Ya existe un usuario con ese correo (RF-8). */
public class EmailYaRegistrado extends RuntimeException {

	public EmailYaRegistrado(String email) {
		super("Ya existe un usuario con el correo " + email);
	}
}
