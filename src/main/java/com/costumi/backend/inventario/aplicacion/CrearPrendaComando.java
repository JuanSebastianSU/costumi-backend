package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.TipoArticulo;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Datos para crear una Prenda en la empresa del usuario autenticado (RF-2.1, RF-2.10). {@code etiquetas}
 * son los valores de etiqueta que la clasifican (RF-2.7, Capa 2); vacía/ausente = prenda sin clasificar.
 */
public record CrearPrendaComando(UUID empresaId, UUID categoriaId, String nombre, TipoArticulo tipoArticulo,
		BigDecimal precioRenta, BigDecimal precioVenta, BigDecimal costoAdquisicion, BigDecimal depositoSugerido,
		BigDecimal valorReposicion, BigDecimal valorDano, List<EtiquetaSeleccionada> etiquetas) {

	public CrearPrendaComando {
		etiquetas = (etiquetas == null) ? List.of() : List.copyOf(etiquetas);
	}
}
