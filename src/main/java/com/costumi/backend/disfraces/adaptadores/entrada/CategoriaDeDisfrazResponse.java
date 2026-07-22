package com.costumi.backend.disfraces.adaptadores.entrada;

import com.costumi.backend.disfraces.dominio.CategoriaDeDisfraz;

import java.util.UUID;

/** DTO de salida de la Categoría de disfraz. */
public record CategoriaDeDisfrazResponse(UUID id, UUID empresaId, String nombre, boolean archivada) {

	static CategoriaDeDisfrazResponse desde(CategoriaDeDisfraz categoria) {
		return new CategoriaDeDisfrazResponse(categoria.id(), categoria.empresaId(), categoria.nombre(),
				categoria.archivada());
	}
}
