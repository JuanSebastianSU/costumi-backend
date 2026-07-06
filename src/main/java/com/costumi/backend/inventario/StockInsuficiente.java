package com.costumi.backend.inventario;

import java.util.UUID;

/** No hay suficientes unidades disponibles de la prenda para descontar (RF-4.4). Se traduce a 409. */
public class StockInsuficiente extends RuntimeException {

	public StockInsuficiente(UUID prendaId) {
		super("No hay suficiente stock disponible de la prenda " + prendaId);
	}
}
