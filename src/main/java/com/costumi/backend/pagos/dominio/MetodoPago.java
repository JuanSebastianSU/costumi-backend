package com.costumi.backend.pagos.dominio;

/** Método de pago (RF-6.7). Lista extensible; efectivo entra a caja física, los demás se concilian. */
public enum MetodoPago {

	EFECTIVO,
	TARJETA,
	TRANSFERENCIA
}
