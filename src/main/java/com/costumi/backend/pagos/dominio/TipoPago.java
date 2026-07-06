package com.costumi.backend.pagos.dominio;

/**
 * Naturaleza de un pago:
 * <ul>
 *   <li>{@code COBRO}: entra dinero como ingreso de la operación (RF-6.1).</li>
 *   <li>{@code REEMBOLSO}: se devuelve dinero al cliente (resta del ingreso) (RF-6.9).</li>
 *   <li>{@code DEPOSITO}: garantía retenida; <b>no es ingreso</b>, se rastrea aparte hasta
 *       liquidarse (RF-6.2/6.8).</li>
 *   <li>{@code DEVOLUCION_DEPOSITO}: devolución del remanente de la garantía; tampoco es ingreso,
 *       libera la retención (RF-6.8).</li>
 * </ul>
 */
public enum TipoPago {
	COBRO,
	REEMBOLSO,
	DEPOSITO,
	DEVOLUCION_DEPOSITO
}
