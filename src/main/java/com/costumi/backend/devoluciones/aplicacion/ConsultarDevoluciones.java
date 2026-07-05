package com.costumi.backend.devoluciones.aplicacion;

import com.costumi.backend.devoluciones.dominio.Devolucion;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: lista las Devoluciones de una empresa (scoped por tenant). */
public interface ConsultarDevoluciones {

	List<Devolucion> deEmpresa(UUID empresaId);
}
