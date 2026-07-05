package com.costumi.backend.catalogo.aplicacion;

import com.costumi.backend.catalogo.dominio.TipoEtiqueta;

import java.util.UUID;

/** Puerto de entrada: renombrar un tipo de etiqueta (RF-2.7.6); propaga por id. */
public interface RenombrarTipoEtiqueta {

	TipoEtiqueta ejecutar(UUID empresaId, UUID tipoEtiquetaId, String nuevoNombre);
}
