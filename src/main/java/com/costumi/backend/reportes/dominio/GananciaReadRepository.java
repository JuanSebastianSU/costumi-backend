package com.costumi.backend.reportes.dominio;

import java.time.LocalDate;
import java.util.UUID;

/** Puerto de salida: modelo de lectura de la ganancia de la empresa (tenant). */
public interface GananciaReadRepository {

	/** Ganancia de la empresa (opcionalmente por sucursal y por rango {@code [desde, hasta]}). */
	ResumenDeGanancia deEmpresa(UUID empresaId, UUID sucursalId, LocalDate desde, LocalDate hasta);
}
