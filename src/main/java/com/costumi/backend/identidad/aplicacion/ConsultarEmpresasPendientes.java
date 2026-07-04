package com.costumi.backend.identidad.aplicacion;

import java.util.List;

/** Puerto de entrada: cola de solicitudes PENDIENTES para el SuperAdmin (RF-15.3, RF-15.4). */
public interface ConsultarEmpresasPendientes {

	List<EmpresaPendienteVista> ejecutar();
}
