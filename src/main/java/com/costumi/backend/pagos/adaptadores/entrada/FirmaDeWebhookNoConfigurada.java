package com.costumi.backend.pagos.adaptadores.entrada;

/**
 * El webhook de pagos no tiene secreto de firma configurado (SEC-5). Fail-closed: sin secreto no se aceptan
 * confirmaciones de pago (evita que una mala configuración deje el webhook abierto). Se traduce a HTTP 503.
 */
class FirmaDeWebhookNoConfigurada extends RuntimeException {

	FirmaDeWebhookNoConfigurada() {
		super("El webhook de pagos no está configurado (falta el secreto de firma)");
	}
}
