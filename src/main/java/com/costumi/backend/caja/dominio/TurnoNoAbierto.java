package com.costumi.backend.caja.dominio;

/** Se intentó operar (mover/cerrar) sobre un turno que no está abierto (RF-6.3). Se traduce a 409. */
public class TurnoNoAbierto extends RuntimeException {

	public TurnoNoAbierto() {
		super("El turno de caja no está abierto");
	}
}
