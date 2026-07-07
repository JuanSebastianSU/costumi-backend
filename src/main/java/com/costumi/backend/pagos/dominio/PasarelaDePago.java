package com.costumi.backend.pagos.dominio;

import java.math.BigDecimal;

/** Puerto de salida: pasarela de pago en línea (RF-6.11). Lo implementa un adaptador (MercadoPago/Stripe). */
public interface PasarelaDePago {

	/** ¿Está configurada con credenciales? */
	boolean configurada();

	/**
	 * Crea un checkout externo para cobrar {@code monto} y devuelve la URL y el id externo del checkout.
	 * Lanza {@link PasarelaNoConfigurada} si no hay credencial.
	 */
	ResultadoCheckout crearCheckout(BigDecimal monto, String moneda, String referencia, String descripcion);

	record ResultadoCheckout(String urlCheckout, String idExterno) {
	}
}
