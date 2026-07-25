package com.costumi.backend.pagos.adaptadores.entrada;

import com.costumi.backend.compartido.CodigoDeRetiro;
import com.costumi.backend.pagos.dominio.Pago;
import com.costumi.backend.pagos.dominio.TipoConcepto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** DTO de salida del Pago. {@code codigoRetiro} es el de la operación cobrada (para mostrarlo tras el cobro). */
public record PagoResponse(UUID id, UUID sucursalId, UUID empleadoId, String tipoConcepto, UUID conceptoId,
		BigDecimal monto, String tipoPago, String metodo, String referencia, Instant fecha, String codigoRetiro) {

	static PagoResponse desde(Pago p) {
		return new PagoResponse(p.id(), p.sucursalId(), p.empleadoId(), p.tipoConcepto().name(), p.conceptoId(),
				p.monto(), p.tipoPago().name(), p.metodo().name(), p.referencia(), p.fecha(),
				CodigoDeRetiro.de(p.tipoConcepto() == TipoConcepto.RENTA ? "R" : "V", p.conceptoId()));
	}
}
