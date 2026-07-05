package com.costumi.backend.disfraces.dominio;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Puerto que el dominio del Disfraz usa para calcular su <b>disponibilidad derivada</b> (RF-2.4) sin
 * conocer el inventario: responde si hay stock disponible para una prenda fija o para un pool
 * (categoría + etiquetas permitidas). La implementación real vive en la capa de aplicación y consulta
 * Inventario; en los tests de dominio se sustituye por un stub.
 */
public interface ConsultaDeStockDePool {

	/** ¿La prenda fija tiene al menos una unidad disponible? */
	boolean prendaTieneStock(UUID prendaId);

	/**
	 * ¿El pool (prendas de la categoría cuyas etiquetas satisfacen los valores permitidos) tiene al
	 * menos una unidad disponible? {@code etiquetasPermitidas} vacío = cualquier prenda de la categoría.
	 */
	boolean poolTieneStock(UUID categoriaId, Map<UUID, Set<UUID>> etiquetasPermitidas);
}
