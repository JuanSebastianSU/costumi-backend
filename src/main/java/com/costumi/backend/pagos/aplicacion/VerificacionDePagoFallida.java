package com.costumi.backend.pagos.aplicacion;

/**
 * La verificación del pago contra el proveedor (P-3) no cuadró: el monto informado por la pasarela no
 * coincide con el del intento. Es una anomalía (posible manipulación), no un pago pendiente. Se traduce a 409.
 */
public class VerificacionDePagoFallida extends RuntimeException {

	public VerificacionDePagoFallida(String detalle) {
		super(detalle);
	}
}
