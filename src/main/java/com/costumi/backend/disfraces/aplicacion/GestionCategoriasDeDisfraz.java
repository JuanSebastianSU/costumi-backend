package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.CategoriaDeDisfraz;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada: gestión de las Categorías de disfraz (listar, crear, renombrar, archivar/activar),
 * acotada a la empresa (tenant). Es una taxonomía aparte de las categorías de prenda.
 */
public interface GestionCategoriasDeDisfraz {

	List<CategoriaDeDisfraz> deEmpresa(UUID empresaId);

	CategoriaDeDisfraz crear(UUID empresaId, String nombre);

	CategoriaDeDisfraz renombrar(UUID empresaId, UUID categoriaId, String nuevoNombre);

	CategoriaDeDisfraz archivar(UUID empresaId, UUID categoriaId);

	CategoriaDeDisfraz activar(UUID empresaId, UUID categoriaId);
}
