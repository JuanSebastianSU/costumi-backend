package com.costumi.backend.ventas.dominio;

import java.util.UUID;

/** La venta no se puede devolver porque no está CONFIRMADA (ya fue devuelta u otro estado) — RF-4.5. */
public class VentaNoDevolvible extends RuntimeException {

	public VentaNoDevolvible(UUID ventaId) {
		super("La venta " + ventaId + " no se puede devolver en su estado actual");
	}
}
