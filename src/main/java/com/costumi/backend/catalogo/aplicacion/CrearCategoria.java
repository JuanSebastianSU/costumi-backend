package com.costumi.backend.catalogo.aplicacion;

import com.costumi.backend.catalogo.dominio.Categoria;

/** Puerto de entrada: alta de una Categoría (RF-2.8). */
public interface CrearCategoria {

	Categoria ejecutar(CrearCategoriaComando comando);
}
