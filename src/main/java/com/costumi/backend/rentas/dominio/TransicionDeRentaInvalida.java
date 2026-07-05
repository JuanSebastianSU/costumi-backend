package com.costumi.backend.rentas.dominio;

/** Se intentó un cambio de estado no permitido por la máquina de estados de la Renta. */
public class TransicionDeRentaInvalida extends RuntimeException {

	public TransicionDeRentaInvalida(EstadoRenta desde, EstadoRenta hacia) {
		super("Transición de renta inválida: " + desde + " -> " + hacia);
	}
}
