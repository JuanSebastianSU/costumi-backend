package com.costumi.backend.disfraces.aplicacion;

import java.util.List;
import java.util.UUID;

/** Datos para crear un Disfraz: su nombre y la lista de {@code slots} (1..8, fijos o personalizables). */
public record CrearDisfrazComando(UUID empresaId, String nombre, List<SlotComando> slots) {

	public CrearDisfrazComando {
		slots = (slots == null) ? List.of() : List.copyOf(slots);
	}
}
