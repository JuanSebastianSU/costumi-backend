package com.costumi.backend.ventas.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * De qué disfraz salió una línea. Al cobrar, un disfraz se resuelve a las prendas que lo componen; sin
 * esto el disfraz se perdía y el pedido quedaba como un montón de piezas sueltas.
 *
 * <p>{@code grupo} identifica una instancia concreta del disfraz dentro del pedido: el mismo disfraz
 * puede ir dos veces con piezas distintas, y sin el grupo ambas se mezclarían al agrupar.
 * {@code cantidad} es cuántos disfraces de ese grupo se cobraron (las líneas llevan la cantidad de
 * prendas, que es cantidad × piezas). {@code nombre} es el nombre CON EL QUE SE COBRÓ: se guarda, no se
 * resuelve al leer, para que un pedido histórico no cambie si después renombran el disfraz (y para que
 * este módulo no dependa del de Disfraces).
 */
public record OrigenDisfraz(UUID disfrazId, UUID grupo, int cantidad, String nombre) {

	public OrigenDisfraz {
		Objects.requireNonNull(disfrazId, "disfrazId");
		Objects.requireNonNull(grupo, "grupo");
		if (cantidad <= 0) {
			throw new IllegalArgumentException("La cantidad de disfraces debe ser mayor a 0");
		}
	}

	/** Reconstruye desde persistencia; las tres columnas viajan juntas o ninguna. */
	public static OrigenDisfraz rehidratar(UUID disfrazId, UUID grupo, Integer cantidad, String nombre) {
		if (disfrazId == null || grupo == null || cantidad == null) {
			return null;
		}
		return new OrigenDisfraz(disfrazId, grupo, cantidad, nombre);
	}
}
