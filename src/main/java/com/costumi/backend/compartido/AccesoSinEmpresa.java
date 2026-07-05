package com.costumi.backend.compartido;

/** La operación exige un usuario asociado a una empresa (tenant) y el token no la trae. */
public class AccesoSinEmpresa extends RuntimeException {

	public AccesoSinEmpresa() {
		super("La operación requiere un usuario asociado a una empresa");
	}
}
