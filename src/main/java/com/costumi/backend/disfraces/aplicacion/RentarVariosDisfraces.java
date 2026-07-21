package com.costumi.backend.disfraces.aplicacion;

import java.util.UUID;

/**
 * Puerto de entrada: rentar varios disfraces distintos (con cantidad) al mismo cliente en una sola renta
 * (RF-3.1). Resuelve cada disfraz a sus prendas y acumula todas las líneas. Devuelve el id de la renta.
 */
public interface RentarVariosDisfraces {

	UUID ejecutar(RentarVariosDisfracesComando comando);
}
