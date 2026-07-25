package com.costumi.backend.clientes.dominio;

import java.util.Locale;

/**
 * Pestañas de "Mis Pedidos" del cliente (RF-14.4/18.9) por las que se filtra su historial:
 * <ul>
 *   <li>{@code TODOS}: sin filtro.</li>
 *   <li>{@code POR_PAGAR}: la operación aún tiene saldo pendiente (debe dinero), sea renta o venta.</li>
 *   <li>{@code POR_RETIRAR}: renta reservada que todavía no retiró (aún no entregada).</li>
 *   <li>{@code ACTIVOS}: en curso — renta entregada o devuelta sin cerrar; venta vigente no devuelta del todo.</li>
 *   <li>{@code CERRADOS}: terminados — renta cerrada/cancelada; venta totalmente devuelta.</li>
 * </ul>
 * {@code POR_PAGAR} es transversal (mira el saldo); las otras tres miran la categoría de la operación.
 */
public enum FiltroDeHistorial {
	TODOS,
	POR_PAGAR,
	POR_RETIRAR,
	ACTIVOS,
	CERRADOS;

	/** Traduce el parámetro (case-insensitive); null/vacío/desconocido = {@link #TODOS} (sin filtro). */
	public static FiltroDeHistorial desde(String valor) {
		if (valor == null || valor.isBlank()) {
			return TODOS;
		}
		try {
			return valueOf(valor.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException e) {
			return TODOS;
		}
	}
}
