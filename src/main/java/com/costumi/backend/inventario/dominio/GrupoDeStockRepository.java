package com.costumi.backend.inventario.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de Grupos de stock (scoped por tenant). */
public interface GrupoDeStockRepository {

	GrupoDeStock guardar(GrupoDeStock grupoDeStock);

	Optional<GrupoDeStock> buscarPorId(UUID id);

	List<GrupoDeStock> listarPorPrenda(UUID prendaId);
}
