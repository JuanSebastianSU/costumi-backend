package com.costumi.backend.inventario.dominio;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Puerto de salida: modelo de lectura del catálogo de inventario del tenant. Lista las prendas activas
 * de la empresa filtradas por categoría (opcional) y por valores de etiqueta por dimensión (opcional,
 * {@code tipoEtiquetaId -> valores permitidos}, en AND entre dimensiones y OR dentro de cada dimensión),
 * con su stock disponible y sus etiquetas.
 */
public interface CatalogoDeInventarioReadRepository {

	List<PrendaDeCatalogo> buscar(UUID empresaId, UUID categoriaId, Map<UUID, Set<UUID>> etiquetas);
}
