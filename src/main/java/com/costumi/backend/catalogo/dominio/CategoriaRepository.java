package com.costumi.backend.catalogo.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de Categorías (scoped por tenant). */
public interface CategoriaRepository {

	Categoria guardar(Categoria categoria);

	Optional<Categoria> buscarPorId(UUID id);

	List<Categoria> listarPorEmpresa(UUID empresaId);
}
