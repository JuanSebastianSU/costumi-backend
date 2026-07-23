package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.EjeDePrenda;

import java.math.BigDecimal;
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
			List<OpcionElegible> opciones) {
	}

	/**
	 * Una opción concreta de la "ruleta": la prenda con su foto, precio, stock y sus etiquetas ya resueltas a
	 * nombre ("Talla: M") para que el cliente pueda distinguir opciones que solo difieren en talla o color.
	 */
	record OpcionElegible(UUID prendaId, String nombre, String fotoUrl, BigDecimal precioRenta,
			int unidadesDisponibles, List<EtiquetaConNombre> etiquetas) {
	}

	/** Una etiqueta de la opción con tipo y valor legibles (ej. tipo "Talla", valor "M"). */
	record EtiquetaConNombre(UUID tipoEtiquetaId, String tipoNombre, UUID valorEtiquetaId, String valorNombre) {
	}
}
