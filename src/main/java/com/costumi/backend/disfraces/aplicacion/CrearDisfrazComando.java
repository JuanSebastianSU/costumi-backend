package com.costumi.backend.disfraces.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Datos para crear un Disfraz: su nombre, los {@code slots} (1..8, fijos o personalizables) y, opcional,
 * un {@code precioRentaGeneral} por día que anula la suma por prendas.
 */
public record CrearDisfrazComando(UUID empresaId, String nombre, UUID categoriaId, List<SlotComando> slots,
		BigDecimal precioRentaGeneral, BigDecimal precioVentaGeneral) {

	public CrearDisfrazComando {
		slots = (slots == null) ? List.of() : List.copyOf(slots);
	}
}
