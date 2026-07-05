package com.costumi.backend.catalogo.adaptadores.entrada;

import com.costumi.backend.catalogo.dominio.ValorEtiqueta;

import java.util.UUID;

/** DTO de salida del Valor de etiqueta. */
public record ValorEtiquetaResponse(UUID id, UUID empresaId, UUID tipoEtiquetaId, String valor, boolean archivada) {

	static ValorEtiquetaResponse desde(ValorEtiqueta valor) {
		return new ValorEtiquetaResponse(valor.id(), valor.empresaId(), valor.tipoEtiquetaId(), valor.valor(),
				valor.archivada());
	}
}
