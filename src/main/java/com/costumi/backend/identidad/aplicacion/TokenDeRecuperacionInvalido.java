package com.costumi.backend.identidad.aplicacion;

/** El token de recuperación no existe, ya venció o ya se usó (RF-1.1). Se traduce a 400. */
public class TokenDeRecuperacionInvalido extends RuntimeException {

	public TokenDeRecuperacionInvalido() {
		super("El enlace de recuperación es inválido o venció. Solicitá uno nuevo.");
	}
}
