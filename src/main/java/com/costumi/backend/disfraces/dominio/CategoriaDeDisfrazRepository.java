package com.costumi.backend.disfraces.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de Categorías de disfraz (scoped por tenant). */
public interface CategoriaDeDisfrazRepository {

	CategoriaDeDisfraz guardar(CategoriaDeDisfraz categoria);

	Optional<CategoriaDeDisfraz> buscarPorId(UUID id);

	List<CategoriaDeDisfraz> listarPorEmpresa(UUID empresaId);
}
