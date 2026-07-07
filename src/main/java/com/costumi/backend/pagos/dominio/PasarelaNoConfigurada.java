package com.costumi.backend.pagos.dominio;

/** La pasarela de pago no tiene credenciales cargadas (RF-6.11). Se traduce a 503. */
public class PasarelaNoConfigurada extends RuntimeException {

	public PasarelaNoConfigurada() {
		super("La pasarela de pago no está configurada. Cargá la credencial (MercadoPago/Stripe).");
	}
}
