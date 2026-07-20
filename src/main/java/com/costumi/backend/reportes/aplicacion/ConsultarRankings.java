package com.costumi.backend.reportes.aplicacion;

import com.costumi.backend.reportes.dominio.ArticuloRanking;
import com.costumi.backend.reportes.dominio.EmpleadoVentas;
import com.costumi.backend.reportes.dominio.ValorEtiquetaRanking;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Puerto de entrada: rankings de artículos, ventas por empleado y desglose por etiqueta (RF-9.1). */
public interface ConsultarRankings {

	List<ArticuloRanking> masVendidos(UUID empresaId, UUID sucursalId, int limite);

	List<ArticuloRanking> masRentados(UUID empresaId, UUID sucursalId, LocalDate desde, LocalDate hasta, int limite);

	List<EmpleadoVentas> ventasPorEmpleado(UUID empresaId, UUID sucursalId);

	List<ValorEtiquetaRanking> ventasPorEtiqueta(UUID empresaId, UUID tipoEtiquetaId, UUID sucursalId);
}
