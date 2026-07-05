package com.costumi.backend.catalogo.aplicacion;

import java.util.List;
import java.util.UUID;

/**
 * Datos para crear un Tipo de etiqueta en la empresa del usuario autenticado (RF-2.7.1/2.7.2).
 * {@code categoriasQueAplica} vacío = el tipo aplica a todas las categorías (dimensión global).
 */
public record CrearTipoEtiquetaComando(UUID empresaId, String nombre, boolean defineVariante,
		boolean seleccionablePorCliente, List<UUID> categoriasQueAplica) {

	public CrearTipoEtiquetaComando {
		categoriasQueAplica = (categoriasQueAplica == null) ? List.of() : List.copyOf(categoriasQueAplica);
	}
}
