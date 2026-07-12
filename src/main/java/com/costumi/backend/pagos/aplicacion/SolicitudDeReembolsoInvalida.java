package com.costumi.backend.pagos.aplicacion;

/** La solicitud de reembolso no es válida: monto mayor al saldo pagado, o ya hay una pendiente (409). */
public class SolicitudDeReembolsoInvalida extends RuntimeException {

	public SolicitudDeReembolsoInvalida(String detalle) {
		super(detalle);
	}
}
