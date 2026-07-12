package com.costumi.backend.pagos.aplicacion;

/**
 * No se puede aprobar el reembolso porque el ítem todavía no está devuelto (RF-4.5): el dinero sigue a la
 * mercancía, no al revés. Se traduce a 409.
 */
public class ItemNoDevuelto extends RuntimeException {

	public ItemNoDevuelto() {
		super("No se puede aprobar el reembolso: el ítem aún no fue devuelto. Registrá la devolución primero.");
	}
}
