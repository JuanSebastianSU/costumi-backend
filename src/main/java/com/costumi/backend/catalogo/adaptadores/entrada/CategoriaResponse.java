package com.costumi.backend.catalogo.adaptadores.entrada;

import com.costumi.backend.catalogo.dominio.Categoria;

import java.util.UUID;

/** DTO de salida de la Categoría. */
public record CategoriaResponse(UUID id, UUID empresaId, String nombre, boolean archivada) {

	static CategoriaResponse desde(Categoria categoria) {
		return new CategoriaResponse(categoria.id(), categoria.empresaId(), categoria.nombre(), categoria.archivada());
	}
}
