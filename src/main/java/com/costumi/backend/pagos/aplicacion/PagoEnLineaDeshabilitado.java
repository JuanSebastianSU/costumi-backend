package com.costumi.backend.pagos.aplicacion;

/** La empresa no tiene habilitado el pago en línea (switch pagoEnLinea apagado, RF-6.11/12.4). Se traduce a 409. */
public class PagoEnLineaDeshabilitado extends RuntimeException {

	public PagoEnLineaDeshabilitado() {
		super("El pago en línea no está habilitado para esta empresa. Activá el switch en Configuración.");
	}
}
