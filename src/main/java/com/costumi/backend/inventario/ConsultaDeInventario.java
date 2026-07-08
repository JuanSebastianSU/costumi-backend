package com.costumi.backend.inventario;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
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

	/** Unidades disponibles de la prenda EN una sucursal (suma de disponibles de sus grupos en esa sucursal, RF-18.2). */
	int unidadesDisponibles(UUID empresaId, UUID sucursalId, UUID prendaId);

	/** Precio de venta de la prenda (de la empresa), si la prenda existe y lo tiene (RF-16 checkout). */
	Optional<BigDecimal> precioVenta(UUID empresaId, UUID prendaId);

	/** Precio de renta por día de la prenda (de la empresa), si la prenda existe y lo tiene (RF-16 checkout de renta). */
	Optional<BigDecimal> precioRenta(UUID empresaId, UUID prendaId);

	/**
	 * ¿Hay al menos una prenda disponible en el pool? El pool son las prendas de la {@code categoria}
	 * cuyas etiquetas satisfacen los valores permitidos por dimensión ({@code etiquetasPermitidas}
	 * vacío = cualquier prenda de la categoría) y que tienen unidades disponibles.
	 */
	boolean poolTieneStockDisponible(UUID empresaId, UUID categoriaId, Map<UUID, Set<UUID>> etiquetasPermitidas);
}
