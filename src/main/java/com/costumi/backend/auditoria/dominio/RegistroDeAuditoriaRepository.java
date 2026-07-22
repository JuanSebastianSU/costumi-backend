package com.costumi.backend.auditoria.dominio;

import java.util.List;
import java.util.UUID;

/** Puerto de salida: persistencia del registro de auditoría (scoped por tenant). */
public interface RegistroDeAuditoriaRepository {

	RegistroDeAuditoria guardar(RegistroDeAuditoria registro);

	List<RegistroDeAuditoria> listarPorEmpresa(UUID empresaId);

	/**
	 * Página de registros (más recientes primero), filtrando opcionalmente por texto en acción o detalle.
	 * La auditoría crece sin techo: la lista completa no se puede devolver.
	 */
	com.costumi.backend.compartido.Pagina<RegistroDeAuditoria> listarPorEmpresa(UUID empresaId, String buscar,
			com.costumi.backend.compartido.SolicitudDePagina pagina);
}
