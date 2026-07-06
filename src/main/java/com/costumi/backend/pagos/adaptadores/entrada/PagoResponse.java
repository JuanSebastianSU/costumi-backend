package com.costumi.backend.pagos.adaptadores.entrada;

import com.costumi.backend.pagos.dominio.Pago;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** DTO de salida del Pago. */
public record PagoResponse(UUID id, UUID sucursalId, UUID empleadoId, String tipoConcepto, UUID conceptoId,
		BigDecimal monto, String tipoPago, String metodo, String referencia, Instant fecha) {

	static PagoResponse desde(Pago p) {
		return new PagoResponse(p.id(), p.sucursalId(), p.empleadoId(), p.tipoConcepto().name(), p.conceptoId(),
				p.monto(), p.tipoPago().name(), p.metodo().name(), p.referencia(), p.fecha());
	}
}
