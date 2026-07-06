package com.costumi.backend.rentas.aplicacion;

/**
 * No hay unidades disponibles de la prenda para el periodo pedido (todas las unidades están rentadas
 * en fechas que se traslapan, RF-3.2). Se traduce a 409.
 */
public class SinDisponibilidad extends RuntimeException {

	public SinDisponibilidad() {
		super("No hay disponibilidad de la prenda para las fechas indicadas");
	}
}
