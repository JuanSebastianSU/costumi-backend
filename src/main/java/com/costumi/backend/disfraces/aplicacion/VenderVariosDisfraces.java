package com.costumi.backend.disfraces.aplicacion;

import java.util.UUID;

/**
 * Puerto de entrada: vender varios disfraces distintos (con cantidad) al mismo cliente en una sola venta.
 * Resuelve cada disfraz a sus prendas y acumula todas las líneas. Devuelve el id de la venta.
 */
public interface VenderVariosDisfraces {

	UUID ejecutar(VenderVariosDisfracesComando comando);
}
