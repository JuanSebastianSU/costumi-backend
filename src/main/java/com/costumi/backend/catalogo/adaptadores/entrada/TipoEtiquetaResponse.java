package com.costumi.backend.catalogo.adaptadores.entrada;

import com.costumi.backend.catalogo.dominio.TipoEtiqueta;

import java.util.List;
import java.util.UUID;

/** DTO de salida del Tipo de etiqueta, con las categorías a las que aplica (vacío = todas). */
public record TipoEtiquetaResponse(UUID id, UUID empresaId, String nombre, boolean defineVariante,
		boolean seleccionablePorCliente, List<UUID> categoriasQueAplica, boolean archivada) {

	static TipoEtiquetaResponse desde(TipoEtiqueta tipo) {
		return new TipoEtiquetaResponse(tipo.id(), tipo.empresaId(), tipo.nombre(), tipo.defineVariante(),
				tipo.seleccionablePorCliente(), List.copyOf(tipo.categoriasQueAplica()), tipo.archivada());
	}
}
