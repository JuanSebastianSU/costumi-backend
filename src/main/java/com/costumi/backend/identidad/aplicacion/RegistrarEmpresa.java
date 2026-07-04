package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Empresa;

/** Puerto de entrada: caso de uso de auto-registro de una Empresa (RF-15.2). */
public interface RegistrarEmpresa {

	Empresa ejecutar(RegistrarEmpresaComando comando);
}
