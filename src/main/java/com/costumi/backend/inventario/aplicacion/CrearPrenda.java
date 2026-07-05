package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.Prenda;

/** Puerto de entrada: alta de una Prenda en la biblioteca (RF-2). */
public interface CrearPrenda {

	Prenda ejecutar(CrearPrendaComando comando);
}
