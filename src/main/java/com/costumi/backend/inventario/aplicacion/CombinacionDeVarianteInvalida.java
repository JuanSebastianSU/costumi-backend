package com.costumi.backend.inventario.aplicacion;

/**
 * La combinación de valores de etiqueta pedida no es válida: referencia un tipo que no existe o no
 * define variantes, un valor que no pertenece al tipo, o repite una dimensión. Se traduce a 400.
 */
public class CombinacionDeVarianteInvalida extends RuntimeException {

	public CombinacionDeVarianteInvalida(String mensaje) {
		super(mensaje);
	}
}
