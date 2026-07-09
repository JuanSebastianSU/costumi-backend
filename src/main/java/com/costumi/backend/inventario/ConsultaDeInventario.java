package com.costumi.backend.inventario;

import java.math.BigDecimal;
import java.util.List;
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

	/**
	 * ¿La prenda (de la empresa) pertenece al pool? Es decir, es de la {@code categoria} y sus etiquetas
	 * satisfacen los valores permitidos por dimensión ({@code etiquetasPermitidas} vacío = cualquier
	 * prenda de la categoría). Sirve para validar la elección del cliente en un slot personalizable (RF-2.3).
	 */
	boolean prendaEnPool(UUID empresaId, UUID prendaId, UUID categoriaId, Map<UUID, Set<UUID>> etiquetasPermitidas);

	/**
	 * Una prenda concreta elegible en la "ruleta" de un slot: su nombre, precio de renta, unidades
	 * disponibles (a nivel empresa) y sus valores de etiqueta ({@code tipoEtiquetaId -> valorEtiquetaId})
	 * para que el cliente pueda filtrar por talla/color/modelo.
	 */
	record OpcionDePool(UUID prendaId, String nombre, BigDecimal precioRenta, int unidadesDisponibles,
			Map<UUID, UUID> etiquetas) {
	}

	/**
	 * Prendas concretas del pool con stock disponible (RF-2.3, "ruleta"): las de la {@code categoria}
	 * cuyas etiquetas satisfacen los valores permitidos ({@code etiquetasPermitidas} vacío = cualquiera de
	 * la categoría) y que tienen al menos una unidad disponible. Ordenadas por nombre.
	 */
	List<OpcionDePool> opcionesDelPool(UUID empresaId, UUID categoriaId, Map<UUID, Set<UUID>> etiquetasPermitidas);

	/** La opción concreta de una prenda fija (nombre, precio, stock y etiquetas), si existe en la empresa. */
	Optional<OpcionDePool> opcionDePrenda(UUID empresaId, UUID prendaId);

	/**
	 * Cuántas prendas <b>activas</b> (no archivadas) de la empresa pertenecen a la categoría. Sirve para
	 * que la UI confirme el impacto antes de archivar una categoría (RF-2.8): "esto afecta a N prendas".
	 */
	int contarPrendasEnCategoria(UUID empresaId, UUID categoriaId);

	/**
	 * Cuántas prendas <b>activas</b> de la empresa llevan alguna etiqueta de ese tipo. Sirve para confirmar
	 * el impacto antes de archivar un tipo de etiqueta (RF-2.7.6).
	 */
	int contarPrendasConTipoEtiqueta(UUID empresaId, UUID tipoEtiquetaId);

	/**
	 * Cuántas prendas <b>activas</b> de la empresa llevan ese valor de etiqueta. Sirve para confirmar el
	 * impacto antes de archivar un valor de etiqueta (RF-2.7.6).
	 */
	int contarPrendasConValorEtiqueta(UUID empresaId, UUID valorEtiquetaId);

	/**
	 * Cuántas <b>unidades</b> de stock (en cualquier estado) hay en la sucursal (de la empresa). Sirve para
	 * impedir archivar una sucursal que todavía tiene inventario (RF-15.1) y reportar cuánto queda.
	 */
	int contarUnidadesEnSucursal(UUID empresaId, UUID sucursalId);
}
