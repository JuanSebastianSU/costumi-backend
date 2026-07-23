package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.TipoDeDisfraz;

import java.util.UUID;

/**
 * Una pieza del disfraz no sirve para lo que sirve el disfraz (RF-2.1 + RF-2.3): un disfraz de renta no
 * puede llevar prendas de solo venta, uno de venta no puede llevar prendas de solo renta, y uno de AMBOS
 * exige piezas que sirvan para las dos cosas.
 *
 * <p>El mensaje dice <b>qué hacer</b>, no solo qué falló: el dueño tiene dos salidas (cambiar la pieza, o
 * cambiar el tipo de artículo de esa prenda) y desde el error a secas no se adivinan.
 */
public class PrendaNoSirveParaElDisfraz extends RuntimeException {

	public PrendaNoSirveParaElDisfraz(UUID prendaId, TipoDeDisfraz tipo, String descripcion) {
		super(descripcion + " (" + prendaId + ") no sirve para un disfraz de " + etiqueta(tipo)
				+ ": la prenda debe ser de tipo " + exigido(tipo)
				+ ". Cambiá la pieza por otra, o editá el tipo de artículo de esa prenda.");
	}

	private static String etiqueta(TipoDeDisfraz tipo) {
		return switch (tipo) {
			case RENTA -> "renta";
			case VENTA -> "venta";
			case AMBOS -> "renta y venta";
		};
	}

	/** Qué tipo de artículo se le exige a la prenda para ese disfraz. */
	private static String exigido(TipoDeDisfraz tipo) {
		return switch (tipo) {
			case RENTA -> "RENTA o AMBOS";
			case VENTA -> "VENTA o AMBOS";
			case AMBOS -> "AMBOS";
		};
	}
}
