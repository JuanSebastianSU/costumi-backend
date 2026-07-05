package com.costumi.backend.devoluciones.aplicacion;

import com.costumi.backend.devoluciones.dominio.Devolucion;

/** Puerto de entrada: registrar la devolución de una renta (RF-5). */
public interface RegistrarDevolucion {

	Devolucion ejecutar(RegistrarDevolucionComando comando);
}
