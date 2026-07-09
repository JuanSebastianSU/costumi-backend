package com.costumi.backend.catalogo.aplicacion;

import com.costumi.backend.catalogo.dominio.Categoria;

import java.util.UUID;

/** Puerto de entrada: editar (renombrar) y archivar/activar una Categoría (RF-2.8/2.7.6), acotado al tenant. */
public interface GestionarCategoria {

	/** Renombra la categoría (misma categoría, otro nombre; propaga). */
	Categoria renombrar(UUID empresaId, UUID categoriaId, String nuevoNombre);

	/** La archiva: deja de ofrecerse para clasificar/crear, pero conserva el histórico. */
	Categoria archivar(UUID empresaId, UUID categoriaId);

	/** Reactiva una categoría archivada. */
	Categoria activar(UUID empresaId, UUID categoriaId);
}
