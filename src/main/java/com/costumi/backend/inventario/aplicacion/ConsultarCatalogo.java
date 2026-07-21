package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.PrendaDeCatalogo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Caso de uso: listar el catálogo de prendas del tenant, filtrado por categoría (opcional) y por
 * valores de etiqueta por dimensión (opcional), con su stock disponible y sus etiquetas.
 */
public interface ConsultarCatalogo {

	List<PrendaDeCatalogo> ejecutar(UUID empresaId, UUID categoriaId, Map<UUID, Set<UUID>> etiquetas);
}
