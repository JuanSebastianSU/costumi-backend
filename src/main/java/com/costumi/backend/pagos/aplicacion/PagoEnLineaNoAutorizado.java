package com.costumi.backend.pagos.aplicacion;

/**
 * Un cliente intentó iniciar el pago en línea de una operación que no es suya (o no existe a su nombre en
 * esa empresa). Se traduce a 403: no se revela nada del pedido ajeno (paralelo a {@link ReembolsoNoAutorizado}).
 */
public class PagoEnLineaNoAutorizado extends RuntimeException {

	public PagoEnLineaNoAutorizado() {
		super("La operación no existe a tu nombre en esta empresa");
	}
}
