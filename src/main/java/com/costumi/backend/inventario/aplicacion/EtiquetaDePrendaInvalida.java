package com.costumi.backend.inventario.aplicacion;

/**
 * Una etiqueta asignada a la prenda no es válida: referencia un tipo o valor que no existe en la
 * empresa, un valor que no pertenece al tipo, o repite una dimensión. Se traduce a 400.
 */
public class EtiquetaDePrendaInvalida extends RuntimeException {

	public EtiquetaDePrendaInvalida(String mensaje) {
		super(mensaje);
	}
}
