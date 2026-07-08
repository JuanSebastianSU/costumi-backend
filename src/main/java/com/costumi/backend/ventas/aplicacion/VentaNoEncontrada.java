package com.costumi.backend.ventas.aplicacion;

import java.util.UUID;

/** La venta no existe en la empresa (tenant) — mapea a 404. */
public class VentaNoEncontrada extends RuntimeException {

	public VentaNoEncontrada(UUID ventaId) {
		super("La venta " + ventaId + " no existe en esta empresa");
	}
}
