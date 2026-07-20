package com.costumi.backend.reportes.dominio;

import java.util.UUID;

/** Puerto de salida: modelo de lectura de ingresos (consulta la data existente, sin escritura). */
public interface IngresosReadRepository {

	/** Ingresos de la empresa; si {@code sucursalId} no es null, solo de esa sucursal (RF-9.1). */
	ResumenDeIngresos deEmpresa(UUID empresaId, UUID sucursalId);
}
