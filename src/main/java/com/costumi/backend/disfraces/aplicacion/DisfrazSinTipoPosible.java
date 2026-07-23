package com.costumi.backend.disfraces.aplicacion;

/**
 * El disfraz mezcla piezas de <b>solo renta</b> con piezas de <b>solo venta</b>, así que no sirve para
 * nada: no se puede rentar (alguna pieza no se renta) ni vender (alguna pieza no se vende).
 *
 * <p>Solo aparece cuando el dueño <b>no</b> eligió el tipo y se intenta derivar de las piezas. Si lo
 * eligió, el error es el de la pieza concreta que no encaja ({@link PrendaNoSirveParaElDisfraz}), que
 * dice cuál es.
 */
public class DisfrazSinTipoPosible extends RuntimeException {

	public DisfrazSinTipoPosible() {
		super("Este disfraz mezcla prendas de solo renta con prendas de solo venta, así que no se podría "
				+ "ni rentar ni vender. Dejá todas las piezas del mismo tipo, o marcá como AMBOS las que "
				+ "sirvan para las dos cosas.");
	}
}
