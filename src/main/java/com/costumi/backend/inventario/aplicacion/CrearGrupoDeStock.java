package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.GrupoDeStock;

/** Puerto de entrada: alta de un Grupo de stock para una Prenda (RF-2.2). */
public interface CrearGrupoDeStock {

	GrupoDeStock ejecutar(CrearGrupoDeStockComando comando);
}
