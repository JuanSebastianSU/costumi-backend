package com.costumi.backend.identidad.adaptadores.entrada;

/** Se superó el límite de intentos para un endpoint de autenticación (A2). Se traduce a HTTP 429. */
class DemasiadosIntentos extends RuntimeException {

	DemasiadosIntentos() {
		super("Demasiados intentos. Esperá un momento e intentá de nuevo.");
	}
}
