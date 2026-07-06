package com.costumi.backend.reportes.dominio;

import java.util.UUID;

/** Puerto de salida: modelo de lectura de la ganancia de la empresa (tenant). */
public interface GananciaReadRepository {

	ResumenDeGanancia deEmpresa(UUID empresaId);
}
