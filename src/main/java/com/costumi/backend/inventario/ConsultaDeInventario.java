package com.costumi.backend.inventario;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * API pública del módulo Inventario para consultar <b>disponibilidad de stock</b> desde otros módulos
 * (p. ej. Disfraces, para la disponibilidad derivada RF-2.4) sin conocer sus clases internas. Vive en
 * el paquete base del módulo: es lo único que Inventario expone.
 */
public interface ConsultaDeInventario {

	/** ¿La prenda existe y pertenece a la empresa? (validación de referencia cruzada por tenant, §5.4). */
	boolean prendaExiste(UUID empresaId, UUID prendaId);

	/** ¿La prenda (de la empresa) tiene al menos una unidad disponible en algún grupo de stock? */
	boolean prendaTieneStockDisponible(UUID empresaId, UUID prendaId);

	/**
	 * ¿Hay al menos una prenda disponible en el pool? El pool son las prendas de la {@code categoria}
	 * cuyas etiquetas satisfacen los valores permitidos por dimensión ({@code etiquetasPermitidas}
	 * vacío = cualquier prenda de la categoría) y que tienen unidades disponibles.
	 */
	boolean poolTieneStockDisponible(UUID empresaId, UUID categoriaId, Map<UUID, Set<UUID>> etiquetasPermitidas);
}
