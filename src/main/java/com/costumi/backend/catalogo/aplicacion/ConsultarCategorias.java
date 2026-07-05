package com.costumi.backend.catalogo.aplicacion;

import com.costumi.backend.catalogo.dominio.Categoria;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: lista las Categorías de una empresa (scoped por tenant). */
public interface ConsultarCategorias {

	List<Categoria> deEmpresa(UUID empresaId);
}
