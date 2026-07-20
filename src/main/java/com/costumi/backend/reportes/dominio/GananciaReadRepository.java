package com.costumi.backend.reportes.dominio;

import java.util.UUID;

/** Puerto de salida: modelo de lectura de la ganancia de la empresa (tenant). */
public interface GananciaReadRepository {

	/** Ganancia de la empresa; si {@code sucursalId} no es null, solo de esa sucursal (RF-9). */
	ResumenDeGanancia deEmpresa(UUID empresaId, UUID sucursalId);
}
