package com.costumi.backend.clientes.dominio;

/**
 * Una línea del estado de cuenta del cliente <b>con la tienda a la que corresponde</b> (RF-7/11.5).
 *
 * <p>El estado de cuenta normal es de una empresa (lo mira la tienda). El del propio cliente cruza
 * tiendas —tiene una ficha en cada una—, así que sin el nombre de la empresa no se podría saber a quién
 * le debe cada multa.
 */
public record DeudaEnTienda(java.util.UUID empresaId, String empresaNombre, LineaDeEstadoDeCuenta linea) {
}
