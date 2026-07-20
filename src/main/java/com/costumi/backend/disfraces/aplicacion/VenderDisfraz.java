package com.costumi.backend.disfraces.aplicacion;

import java.util.UUID;

/**
 * Puerto de entrada: vender un disfraz. Resuelve el disfraz a sus prendas concretas (la fija de cada slot
 * o la elegida por el cliente en los personalizables) y crea una venta con el precio de venta de cada
 * prenda. Devuelve el id de la venta creada.
 */
public interface VenderDisfraz {

	UUID ejecutar(VenderDisfrazComando comando);
}
