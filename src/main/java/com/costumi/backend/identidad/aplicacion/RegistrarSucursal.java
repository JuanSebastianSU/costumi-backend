package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Sucursal;

/** Puerto de entrada: alta de una Sucursal para una Empresa (RF-15.1). */
public interface RegistrarSucursal {

	Sucursal ejecutar(RegistrarSucursalComando comando);
}
