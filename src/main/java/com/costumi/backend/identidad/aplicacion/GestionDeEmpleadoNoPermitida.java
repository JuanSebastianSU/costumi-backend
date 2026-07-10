package com.costumi.backend.identidad.aplicacion;

/**
 * Un actor intentó gestionar o crear a un empleado que <b>no está estrictamente por debajo</b> suyo en la
 * pirámide de roles (RF-1.3, B3): un igual, un superior o a sí mismo. Se traduce a HTTP 403. Evita que, por
 * ejemplo, un Encargado se re-conceda permisos que el Dueño le quitó, gestione a otro Encargado o cree un Dueño.
 */
public class GestionDeEmpleadoNoPermitida extends RuntimeException {

	public GestionDeEmpleadoNoPermitida() {
		super("No tienes autoridad para gestionar a este empleado o crear ese rol");
	}
}
