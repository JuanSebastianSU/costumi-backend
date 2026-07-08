package com.costumi.backend.disfraces.aplicacion;

import java.util.UUID;

/**
 * Puerto de entrada: rentar un disfraz (RF-2.3/3.1). Resuelve el disfraz a sus prendas concretas
 * (unidad fija = su prenda; por partes = la prenda fija de cada slot o la elegida por el cliente en
 * los personalizables) y crea una renta multi-artículo. Devuelve el id de la renta creada.
 */
public interface RentarDisfraz {

	UUID ejecutar(RentarDisfrazComando comando);
}
