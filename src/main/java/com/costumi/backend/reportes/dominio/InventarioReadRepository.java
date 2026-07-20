package com.costumi.backend.reportes.dominio;

import java.util.List;
import java.util.UUID;

/** Puerto de salida de reportes de inventario (RF-9.1/9.3), por tenant. */
public interface InventarioReadRepository {

	/** Tablero por prenda con el conteo por estado; opcionalmente acotado a una sucursal (RF-9.3). */
	List<GrupoInventario> tablero(UUID empresaId, UUID sucursalId);

	/** Resumen agregado: unidades por estado, rentadas ahora, utilización y valor de inventario (RF-9.1). */
	ResumenInventario resumen(UUID empresaId);
}
