package com.costumi.backend.inventario.aplicacion;

/** Ya existe un grupo de stock con esa misma combinación de variante en la prenda. Se traduce a 409. */
public class VarianteDuplicada extends RuntimeException {

	public VarianteDuplicada() {
		super("Ya existe un grupo de stock con esa combinación de variante en la prenda");
	}
}
