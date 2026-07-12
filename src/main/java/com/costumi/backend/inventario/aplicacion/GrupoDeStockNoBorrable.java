package com.costumi.backend.inventario.aplicacion;

/**
 * No se puede borrar físicamente el grupo de stock (R-F): tiene unidades, o es el último grupo de su prenda
 * en la sucursal (borrarlo dejaría sin dónde reingresar las devoluciones). Se traduce a HTTP 409.
 */
public class GrupoDeStockNoBorrable extends RuntimeException {

	public GrupoDeStockNoBorrable(String motivo) {
		super(motivo);
	}
}
