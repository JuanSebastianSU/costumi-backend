package com.costumi.backend.clientes.dominio;

import java.math.BigDecimal;

/**
 * Carga económica de un cliente (RF-7/11.5): cuánto debe todavía ({@code saldoPendiente} = Σ de sus rentas
 * activas/devueltas de max(0, importe + multa − pagado)) y cuánta multa acumuló ({@code multaTotal}). Sirve
 * para que la cartera muestre la cifra junto a cada cliente, no solo el filtro.
 */
public record CargaDeCliente(BigDecimal saldoPendiente, BigDecimal multaTotal) {

	public static CargaDeCliente vacia() {
		return new CargaDeCliente(BigDecimal.ZERO, BigDecimal.ZERO);
	}
}
