package com.costumi.backend.auditoria.dominio;

import java.util.List;
import java.util.UUID;

/** Puerto de salida: persistencia del registro de auditoría (scoped por tenant). */
public interface RegistroDeAuditoriaRepository {

	RegistroDeAuditoria guardar(RegistroDeAuditoria registro);

	List<RegistroDeAuditoria> listarPorEmpresa(UUID empresaId);
}
