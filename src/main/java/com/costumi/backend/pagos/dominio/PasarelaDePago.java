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

	/**
	 * Consulta el estado real de un pago en el proveedor (P-3): permite verificar contra la fuente antes de
	 * dar por bueno un webhook. Lanza {@link PasarelaNoConfigurada} si no hay credencial.
	 */
	EstadoPagoExterno consultarPago(String idPagoExterno);

	record ResultadoCheckout(String urlCheckout, String idExterno) {
	}

	/** Estado de un pago según el proveedor: si está aprobado y por qué monto (P-3). */
	record EstadoPagoExterno(boolean aprobado, BigDecimal monto) {
	}
}
