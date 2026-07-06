package com.costumi.backend.auditoria.aplicacion;

import com.costumi.backend.auditoria.dominio.RegistroDeAuditoria;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: lista el registro de auditoría de la empresa (tenant). */
public interface ConsultarAuditoria {

	List<RegistroDeAuditoria> deEmpresa(UUID empresaId);
}
