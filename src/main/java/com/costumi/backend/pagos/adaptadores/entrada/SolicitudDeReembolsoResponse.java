package com.costumi.backend.pagos.adaptadores.entrada;

import com.costumi.backend.pagos.dominio.EstadoSolicitudReembolso;
import com.costumi.backend.pagos.dominio.SolicitudDeReembolso;
import com.costumi.backend.pagos.dominio.TipoConcepto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** DTO de salida de una solicitud de reembolso. */
public record SolicitudDeReembolsoResponse(UUID id, TipoConcepto tipoConcepto, UUID conceptoId,
		UUID solicitanteClienteId, String solicitanteNombre, BigDecimal monto, String motivoSolicitud,
		EstadoSolicitudReembolso estado, String motivoDecision, UUID decididoPorUsuarioId, String rolDecision,
		Instant creadaEn, Instant decididaEn) {

	/** Con el nombre del solicitante ya resuelto (listados: se resuelve por lote para evitar N+1). */
	static SolicitudDeReembolsoResponse desde(SolicitudDeReembolso s, String solicitanteNombre) {
		return new SolicitudDeReembolsoResponse(s.id(), s.tipoConcepto(), s.conceptoId(), s.solicitanteClienteId(),
				solicitanteNombre, s.monto(), s.motivoSolicitud(), s.estado(), s.motivoDecision(),
				s.decididoPorUsuarioId(), s.rolDecision(), s.creadaEn(), s.decididaEn());
	}

	/** Sin nombre resuelto (respuestas de una sola solicitud recién creada/decidida). */
	static SolicitudDeReembolsoResponse desde(SolicitudDeReembolso s) {
		return desde(s, null);
	}
}
