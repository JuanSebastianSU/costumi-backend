package com.costumi.backend.identidad.aplicacion;

import java.util.UUID;

/**
 * No se puede archivar una sucursal que todavía tiene dependencias operativas (RF-15.1): unidades de
 * stock o rentas vigentes. Se traduce a HTTP 409 y reporta cuántas hay para que la UI lo explique.
 */
public class SucursalConDependencias extends RuntimeException {

	private final int unidadesStock;
	private final int rentasVigentes;

	public SucursalConDependencias(UUID sucursalId, int unidadesStock, int rentasVigentes) {
		super("No se puede archivar la sucursal " + sucursalId + ": tiene " + unidadesStock
				+ " unidad(es) de stock y " + rentasVigentes + " renta(s) vigente(s)");
		this.unidadesStock = unidadesStock;
		this.rentasVigentes = rentasVigentes;
	}

	public int unidadesStock() {
		return unidadesStock;
	}

	public int rentasVigentes() {
		return rentasVigentes;
	}
}
