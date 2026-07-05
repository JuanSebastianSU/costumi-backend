package com.costumi.backend.catalogo.adaptadores.entrada;

import com.costumi.backend.catalogo.dominio.TipoEtiqueta;

import java.util.UUID;

/** DTO de salida del Tipo de etiqueta. */
public record TipoEtiquetaResponse(UUID id, UUID empresaId, String nombre, boolean defineVariante,
		boolean seleccionablePorCliente, boolean archivada) {

	static TipoEtiquetaResponse desde(TipoEtiqueta tipo) {
		return new TipoEtiquetaResponse(tipo.id(), tipo.empresaId(), tipo.nombre(), tipo.defineVariante(),
				tipo.seleccionablePorCliente(), tipo.archivada());
	}
}
