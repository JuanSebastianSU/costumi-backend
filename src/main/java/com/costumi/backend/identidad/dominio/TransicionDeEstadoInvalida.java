package com.costumi.backend.identidad.dominio;

/** Se intentó un cambio de estado no permitido por la máquina de estados de la Empresa. */
public class TransicionDeEstadoInvalida extends RuntimeException {

	public TransicionDeEstadoInvalida(EstadoEmpresa desde, EstadoEmpresa hacia) {
		super("Transición de estado inválida para la empresa: " + desde + " -> " + hacia);
	}
}
