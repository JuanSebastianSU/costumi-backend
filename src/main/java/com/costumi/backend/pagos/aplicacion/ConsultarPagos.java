package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.Pago;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Puerto de entrada: lista los Pagos de un concepto (renta/venta), scoped por tenant. */
public interface ConsultarPagos {

	List<Pago> deConcepto(UUID empresaId, UUID conceptoId);

	/** Saldo neto pagado de la operación (cobros − reembolsos), RF-6.9. */
	BigDecimal saldoNeto(UUID empresaId, UUID conceptoId);

	/** Estado de la garantía retenida de la operación (retenido/devuelto/activo), RF-6.2/6.8. */
	EstadoDeposito estadoDeposito(UUID empresaId, UUID conceptoId);

	/** Comprobante/recibo de la operación: detalle de pagos y totales derivados, RF-6.5. */
	Comprobante comprobante(UUID empresaId, UUID conceptoId);
}
