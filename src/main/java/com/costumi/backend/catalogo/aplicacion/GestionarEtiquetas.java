package com.costumi.backend.catalogo.aplicacion;

import com.costumi.backend.catalogo.dominio.TipoEtiqueta;
import com.costumi.backend.catalogo.dominio.ValorEtiqueta;

import java.util.UUID;

/** Puerto de entrada: archivar/activar tipos y valores de etiqueta (RF-2.7.6), acotado al tenant. */
public interface GestionarEtiquetas {

	TipoEtiqueta archivarTipo(UUID empresaId, UUID tipoEtiquetaId);

	TipoEtiqueta activarTipo(UUID empresaId, UUID tipoEtiquetaId);

	ValorEtiqueta archivarValor(UUID empresaId, UUID tipoEtiquetaId, UUID valorEtiquetaId);

	ValorEtiqueta activarValor(UUID empresaId, UUID tipoEtiquetaId, UUID valorEtiquetaId);
}
