package com.costumi.backend.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Datos para editar una Prenda (RF-2.10): nombre, precios y valores + etiquetas. El {@code tipoArticulo}
 * y la categoría no cambian; los precios se revalidan contra el tipo existente.
 */
public record EditarPrendaComando(UUID empresaId, UUID prendaId, String nombre, BigDecimal precioRenta,
		BigDecimal precioVenta, BigDecimal costoAdquisicion, BigDecimal depositoSugerido, BigDecimal valorReposicion,
		BigDecimal valorDano, List<EtiquetaSeleccionada> etiquetas) {

	public EditarPrendaComando {
		etiquetas = (etiquetas == null) ? List.of() : List.copyOf(etiquetas);
	}
}
