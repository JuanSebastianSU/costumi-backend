package com.costumi.backend.pagos.adaptadores.entrada;

import com.costumi.backend.pagos.aplicacion.Comprobante;
import com.costumi.backend.pagos.aplicacion.EstadoDeposito;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** DTO de salida del comprobante/recibo de una operación (RF-6.5): detalle de pagos y totales. */
public record ComprobanteResponse(UUID conceptoId, List<PagoResponse> pagos, BigDecimal totalCobrado,
		BigDecimal totalReembolsado, BigDecimal saldoNeto, EstadoDeposito deposito) {

	static ComprobanteResponse desde(Comprobante c) {
		return new ComprobanteResponse(c.conceptoId(), c.pagos().stream().map(PagoResponse::desde).toList(),
				c.totalCobrado(), c.totalReembolsado(), c.saldoNeto(), c.deposito());
	}
}
