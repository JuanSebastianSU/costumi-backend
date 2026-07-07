package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.GrupoDeStock;

import java.util.UUID;

/**
 * Puerto de entrada: transferencia de unidades disponibles de un grupo de stock a otra sucursal
 * (RF-10.3). El destino es el grupo de la misma prenda y variante en la sucursal de destino; si no
 * existe todavía, se crea. Devuelve el grupo de origen ya actualizado.
 */
public interface TransferirStock {

	GrupoDeStock ejecutar(TransferirStockComando comando);

	record TransferirStockComando(UUID empresaId, UUID grupoOrigenId, UUID sucursalDestinoId, int cantidad) {
	}
}
