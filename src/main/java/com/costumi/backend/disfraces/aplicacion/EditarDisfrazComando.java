package com.costumi.backend.disfraces.aplicacion;

import java.util.List;
import java.util.UUID;

/** Datos para editar un Disfraz existente: redefine su nombre y su lista de {@code slots} (1..8). */
public record EditarDisfrazComando(UUID empresaId, UUID disfrazId, String nombre, List<SlotComando> slots) {

	public EditarDisfrazComando {
		slots = (slots == null) ? List.of() : List.copyOf(slots);
	}
}
