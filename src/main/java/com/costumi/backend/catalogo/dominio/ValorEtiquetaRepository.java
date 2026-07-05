package com.costumi.backend.catalogo.dominio;

import java.util.List;
import java.util.UUID;

/** Puerto de salida: persistencia de Valores de etiqueta. */
public interface ValorEtiquetaRepository {

	ValorEtiqueta guardar(ValorEtiqueta valorEtiqueta);

	List<ValorEtiqueta> listarPorTipo(UUID tipoEtiquetaId);
}
