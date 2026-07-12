package com.costumi.backend.pagos.adaptadores.entrada;

import com.costumi.backend.pagos.dominio.EstadoSolicitudReembolso;
import com.costumi.backend.pagos.dominio.SolicitudDeReembolso;
import com.costumi.backend.pagos.dominio.TipoConcepto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** DTO de salida de una solicitud de reembolso. */
public record SolicitudDeReembolsoResponse(UUID id, TipoConcepto tipoConcepto, UUID conceptoId,
		UUID solicitanteClienteId, BigDecimal monto, String motivoSolicitud, EstadoSolicitudReembolso estado,
		String motivoDecision, UUID decididoPorUsuarioId, String rolDecision, Instant creadaEn, Instant decididaEn) {

	static SolicitudDeReembolsoResponse desde(SolicitudDeReembolso s) {
		return new SolicitudDeReembolsoResponse(s.id(), s.tipoConcepto(), s.conceptoId(), s.solicitanteClienteId(),
				s.monto(), s.motivoSolicitud(), s.estado(), s.motivoDecision(), s.decididoPorUsuarioId(),
				s.rolDecision(), s.creadaEn(), s.decididaEn());
	}
}
