package com.costumi.backend.inventario.aplicacion;

import java.util.UUID;

/** El Grupo de stock no existe o no pertenece a la empresa del usuario. */
public class GrupoDeStockNoEncontrado extends RuntimeException {

	public GrupoDeStockNoEncontrado(UUID id) {
		super("No existe el grupo de stock " + id + " en esta empresa");
	}
}
