package com.costumi.backend.catalogo.aplicacion;

/**
 * Una categoría indicada como "a la que aplica" un tipo de etiqueta no existe en la empresa del
 * usuario. Se traduce a 400.
 */
public class CategoriaDeTipoInvalida extends RuntimeException {

	public CategoriaDeTipoInvalida(String mensaje) {
		super(mensaje);
	}
}
