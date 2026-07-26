package com.costumi.backend.marketplace.dominio;

/**
 * Una etiqueta de una prenda tal como la ve el cliente en la vitrina: por NOMBRE (p. ej. "Color: Negro"),
 * no por id. Así el cliente sabe qué está comprando (color, talla, estilo o lo que el dueño haya puesto).
 */
public record EtiquetaEnVitrina(String tipo, String valor) {
}
