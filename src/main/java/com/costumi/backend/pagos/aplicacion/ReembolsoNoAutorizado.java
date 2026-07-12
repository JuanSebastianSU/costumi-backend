package com.costumi.backend.pagos.aplicacion;

/**
 * Un cliente intentó solicitar el reembolso de una operación que no es suya (o no existe a su nombre en esa
 * empresa). Se traduce a 403: no se revela nada del pedido ajeno.
 */
public class ReembolsoNoAutorizado extends RuntimeException {

	public ReembolsoNoAutorizado() {
		super("La operación no existe a tu nombre en esta empresa");
	}
}
