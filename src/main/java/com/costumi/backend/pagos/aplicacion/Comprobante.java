package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.Pago;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Comprobante/recibo de una operación (RF-6.5): el detalle de sus pagos y los totales derivados —
 * cobrado, reembolsado, saldo neto y estado de la garantía. Es un modelo de lectura (no persiste).
 * Los impuestos configurables (RF-6.5/12.2) quedan pendientes de decidir su modelo; no se inventan aquí.
 */
public record Comprobante(UUID conceptoId, List<Pago> pagos, BigDecimal totalCobrado, BigDecimal totalReembolsado,
		BigDecimal saldoNeto, EstadoDeposito deposito) {
}
