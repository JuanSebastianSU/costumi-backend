package com.costumi.backend.identidad.aplicacion;

import java.util.List;

/**
 * Puerto de entrada: listado de Empresas gestionables por el SuperAdmin (ACTIVAS y SUSPENDIDAS),
 * para poder suspenderlas o reactivarlas (RF-15.3). Las PENDIENTES tienen su propia cola
 * ({@link ConsultarEmpresasPendientes}); las RECHAZADAS no son accionables.
 */
public interface ConsultarEmpresas {

	List<EmpresaVista> ejecutar();
}
