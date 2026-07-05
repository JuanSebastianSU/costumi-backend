package com.costumi.backend.reportes.dominio;

import java.util.UUID;

/** Puerto de salida: modelo de lectura de ingresos (consulta la data existente, sin escritura). */
public interface IngresosReadRepository {

	ResumenDeIngresos deEmpresa(UUID empresaId);
}
