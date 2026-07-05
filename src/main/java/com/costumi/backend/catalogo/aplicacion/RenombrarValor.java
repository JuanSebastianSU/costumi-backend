package com.costumi.backend.catalogo.aplicacion;

import com.costumi.backend.catalogo.dominio.ValorEtiqueta;

import java.util.UUID;

/** Puerto de entrada: renombrar un valor de etiqueta (RF-2.7.6); propaga por id. */
public interface RenombrarValor {

	ValorEtiqueta ejecutar(UUID empresaId, UUID tipoEtiquetaId, UUID valorEtiquetaId, String nuevoValor);
}
