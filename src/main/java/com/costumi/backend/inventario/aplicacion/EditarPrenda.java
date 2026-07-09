package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.Prenda;

/** Puerto de entrada: edición de una Prenda existente (RF-2.10), acotada al tenant. */
public interface EditarPrenda {

	Prenda ejecutar(EditarPrendaComando comando);
}
