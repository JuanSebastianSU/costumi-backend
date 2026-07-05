package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.ModoDeDisfraz;

import java.util.List;
import java.util.UUID;

/**
 * Datos para crear un Disfraz. Para {@code UNIDAD_FIJA} se usa {@code prendaFijaId}; para
 * {@code POR_PARTES}, la lista de {@code slots} (1..8).
 */
public record CrearDisfrazComando(UUID empresaId, String nombre, ModoDeDisfraz modo, UUID prendaFijaId,
		List<SlotComando> slots) {

	public CrearDisfrazComando {
		slots = (slots == null) ? List.of() : List.copyOf(slots);
	}
}
