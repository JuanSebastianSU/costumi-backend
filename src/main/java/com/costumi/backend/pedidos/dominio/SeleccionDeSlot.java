package com.costumi.backend.pedidos.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Elección de prenda para un slot personalizable de un disfraz agregado al carrito, por su número de
 * orden. Es parte de la clave de agrupación de una línea de disfraz: dos disfraces iguales pero con
 * distinta elección de prendas son líneas distintas.
 */
public record SeleccionDeSlot(int orden, UUID prendaId) {

	public SeleccionDeSlot {
		Objects.requireNonNull(prendaId, "prendaId");
	}
}
