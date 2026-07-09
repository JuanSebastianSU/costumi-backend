package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.Prenda;

import java.util.UUID;

/** Puerto de entrada: archivar/reactivar una Prenda (RF-2), acotado al tenant. */
public interface CambiarEstadoPrenda {

	/** La retira de la operación (renta/venta/pool) sin borrarla; se conserva el historial. */
	Prenda archivar(UUID empresaId, UUID prendaId);

	/** Reactiva una prenda archivada. */
	Prenda activar(UUID empresaId, UUID prendaId);
}
