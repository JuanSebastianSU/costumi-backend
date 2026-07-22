package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.TipoDeDisfraz;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Datos para editar un Disfraz existente: redefine su nombre, sus {@code slots} (1..8) y su
 * {@code precioRentaGeneral} (nulo = vuelve a cobrarse por prendas).
 */
public record EditarDisfrazComando(UUID empresaId, UUID disfrazId, String nombre, UUID categoriaId,
		List<SlotComando> slots, BigDecimal precioRentaGeneral, BigDecimal precioVentaGeneral,
		TipoDeDisfraz tipo) {

	public EditarDisfrazComando {
		slots = (slots == null) ? List.of() : List.copyOf(slots);
	}
}
