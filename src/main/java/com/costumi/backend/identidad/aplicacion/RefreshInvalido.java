package com.costumi.backend.identidad.aplicacion;

/** El token de refresco no es válido (firma/vigencia/tipo) o su usuario ya no existe (RF-1.1). */
public class RefreshInvalido extends RuntimeException {

	public RefreshInvalido() {
		super("El token de refresco no es válido");
	}
}
