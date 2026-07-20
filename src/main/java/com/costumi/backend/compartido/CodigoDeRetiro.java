package com.costumi.backend.compartido;

import java.util.Locale;
import java.util.UUID;

/**
 * Código de retiro/pedido legible que el cliente muestra en la tienda para retirar lo rentado/comprado.
 * Se deriva de forma estable del id de la operación (renta o venta): mismo pedido, mismo código; único
 * en la práctica. No se persiste (es una vista del id), así que no requiere esquema propio.
 */
public final class CodigoDeRetiro {

	private CodigoDeRetiro() {
	}

	/** Ej.: prefijo "R" + id {@code 3804937a-...} -> {@code "R-3804937A"}. */
	public static String de(String prefijo, UUID operacionId) {
		return prefijo + "-" + operacionId.toString().substring(0, 8).toUpperCase(Locale.ROOT);
	}
}
