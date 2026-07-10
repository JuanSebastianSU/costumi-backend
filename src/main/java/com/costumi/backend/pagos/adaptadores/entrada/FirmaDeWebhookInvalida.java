package com.costumi.backend.pagos.adaptadores.entrada;

/** El webhook de pagos llegó sin firma o con una firma que no valida (SEC-5). Se traduce a HTTP 401. */
class FirmaDeWebhookInvalida extends RuntimeException {

	FirmaDeWebhookInvalida() {
		super("La firma del webhook es inválida o falta");
	}
}
