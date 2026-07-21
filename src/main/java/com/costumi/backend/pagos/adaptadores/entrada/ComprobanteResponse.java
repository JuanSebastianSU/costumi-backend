package com.costumi.backend.pagos.adaptadores.entrada;

import com.costumi.backend.pagos.aplicacion.Comprobante;
import com.costumi.backend.pagos.aplicacion.EstadoDeposito;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO de salida del comprobante/recibo de una operación (RF-6.5): detalle de pagos, totales e impuesto.
 * {@code multa} es la multa acumulada de la renta (0 en ventas o rentas sin multa): forma parte de lo que
 * el cliente debe, para que la pantalla de cobros sume la multa al pendiente (RF-6.6/11.5).
 */
public record ComprobanteResponse(UUID conceptoId, List<PagoResponse> pagos, BigDecimal totalCobrado,
		BigDecimal totalReembolsado, BigDecimal saldoNeto, BigDecimal multa, EstadoDeposito deposito,
		BigDecimal tasaImpuesto, BigDecimal baseImponible, BigDecimal impuesto) {

	static ComprobanteResponse desde(Comprobante c, BigDecimal multa) {
		return new ComprobanteResponse(c.conceptoId(), c.pagos().stream().map(PagoResponse::desde).toList(),
				c.totalCobrado(), c.totalReembolsado(), c.saldoNeto(), multa, c.deposito(), c.tasaImpuesto(),
				c.baseImponible(), c.impuesto());
	}
}
