package com.costumi.backend.inventario.aplicacion;

import java.util.List;
import java.util.UUID;

/**
 * Datos para crear un Grupo de stock (variante) de una Prenda con su cantidad inicial.
 * La {@code combinacion} es la lista de selecciones de valor de etiqueta que define la variante
 * (RF-2.7.3); vacía significa la variante única de una prenda sin dimensiones.
 */
public record CrearGrupoDeStockComando(UUID empresaId, UUID prendaId, List<SeleccionVariante> combinacion,
		int cantidadInicial) {

	public CrearGrupoDeStockComando {
		combinacion = (combinacion == null) ? List.of() : List.copyOf(combinacion);
	}
}
