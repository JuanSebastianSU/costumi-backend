package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.EjeDePrenda;
import com.costumi.backend.inventario.ConsultaDeInventario;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada: la "ruleta" de un slot (RF-2.3). Lista las prendas concretas <b>disponibles</b>
 * que el cliente puede elegir para un slot personalizable (o la prenda fija), con su stock y precio,
 * pudiendo filtrar por valores de etiqueta (talla/color/modelo).
 */
public interface ConsultarOpcionesDeSlot {

	/** Opciones de un slot del disfraz; {@code valoresFiltro} (opcional) acota por valores de etiqueta. */
	OpcionesDeSlot opciones(UUID empresaId, UUID disfrazId, int orden, List<UUID> valoresFiltro);

	/** Un slot con sus opciones concretas disponibles. */
	record OpcionesDeSlot(int orden, String nombre, EjeDePrenda ejePrenda, boolean opcional,
			List<ConsultaDeInventario.OpcionDePool> opciones) {
	}
}
