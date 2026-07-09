package com.costumi.backend.ventas.dominio;

import java.util.UUID;

/** La política de reembolso del local no permite devolver esta venta (deshabilitado o fuera de la ventana) — RF-4.5. */
public class ReembolsoNoPermitido extends RuntimeException {

	public ReembolsoNoPermitido(UUID ventaId) {
		super("El reembolso de la venta " + ventaId + " no está permitido por la política del local");
	}
}
