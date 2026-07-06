package com.costumi.backend.auditoria.aplicacion;

import java.util.UUID;

/** Puerto de entrada: deja constancia de una acción en la auditoría (RF-0.5). */
public interface RegistrarAuditoria {

	void registrar(UUID empresaId, String accion, String detalle);
}
