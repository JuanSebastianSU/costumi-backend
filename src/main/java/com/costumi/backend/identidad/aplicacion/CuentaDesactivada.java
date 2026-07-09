package com.costumi.backend.identidad.aplicacion;

/** La cuenta existe y la credencial es válida, pero está dada de baja: no puede operar (RF-8). */
public class CuentaDesactivada extends RuntimeException {

	public CuentaDesactivada() {
		super("La cuenta está desactivada");
	}
}
