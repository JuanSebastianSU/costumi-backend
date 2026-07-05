package com.costumi.backend.identidad.aplicacion;

/** Email inexistente o contraseña incorrecta. Mensaje genérico: no revela cuál de los dos. */
public class CredencialesInvalidas extends RuntimeException {

	public CredencialesInvalidas() {
		super("Credenciales inválidas");
	}
}
