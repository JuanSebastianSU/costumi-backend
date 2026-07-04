package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Empresa;

import java.util.UUID;

/**
 * Puerto de entrada: decisiones del SuperAdmin sobre el ciclo de vida de una Empresa
 * (RF-15.3). Cada operación aplica una transición de estado y persiste el resultado.
 */
public interface GestionarEmpresa {

	Empresa aprobar(UUID id);

	Empresa rechazar(UUID id);

	Empresa suspender(UUID id);

	Empresa reactivar(UUID id);
}
