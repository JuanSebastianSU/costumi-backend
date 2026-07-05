package com.costumi.backend.catalogo.aplicacion;

import com.costumi.backend.catalogo.dominio.TipoEtiqueta;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: lista los Tipos de etiqueta de una empresa (scoped por tenant). */
public interface ConsultarTiposEtiqueta {

	List<TipoEtiqueta> deEmpresa(UUID empresaId);
}
