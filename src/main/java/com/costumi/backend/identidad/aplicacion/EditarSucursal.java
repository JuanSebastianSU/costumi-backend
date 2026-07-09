package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Sucursal;

/** Puerto de entrada: editar los datos de una sucursal (RF-15.1). */
public interface EditarSucursal {

	Sucursal ejecutar(EditarSucursalComando comando);
}
