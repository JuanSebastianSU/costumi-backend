package com.costumi.backend.catalogo.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de Tipos de etiqueta (scoped por tenant). */
public interface TipoEtiquetaRepository {

	TipoEtiqueta guardar(TipoEtiqueta tipoEtiqueta);

	Optional<TipoEtiqueta> buscarPorId(UUID id);

	List<TipoEtiqueta> listarPorEmpresa(UUID empresaId);
}
