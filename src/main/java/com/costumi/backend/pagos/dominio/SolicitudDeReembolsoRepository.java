package com.costumi.backend.pagos.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de solicitudes de reembolso (scoped por tenant). */
public interface SolicitudDeReembolsoRepository {

	SolicitudDeReembolso guardar(SolicitudDeReembolso solicitud);

	Optional<SolicitudDeReembolso> buscarPorId(UUID id);

	/** Solicitudes de la empresa, más recientes primero (para la bandeja del personal). */
	List<SolicitudDeReembolso> listarPorEmpresa(UUID empresaId);

	/** ¿Ya hay una solicitud PENDIENTE para ese concepto? (evita duplicados). */
	boolean existePendientePorConcepto(UUID empresaId, UUID conceptoId);
}
