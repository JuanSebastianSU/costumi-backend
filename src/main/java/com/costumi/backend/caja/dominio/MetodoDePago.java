package com.costumi.backend.caja.dominio;

/** Método de pago de un movimiento de caja (RF-6.10). El efectivo es el único que se arquea físico. */
public enum MetodoDePago {
	EFECTIVO,
	TARJETA,
	TRANSFERENCIA
}
