package com.costumi.backend.reportes.aplicacion;

import com.costumi.backend.reportes.dominio.ArticuloRanking;
import com.costumi.backend.reportes.dominio.EmpleadoVentas;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Puerto de entrada: rankings de artículos y ventas por empleado (RF-9.1). */
public interface ConsultarRankings {

	List<ArticuloRanking> masVendidos(UUID empresaId, UUID sucursalId, int limite);

	List<ArticuloRanking> masRentados(UUID empresaId, UUID sucursalId, LocalDate desde, LocalDate hasta, int limite);

	List<EmpleadoVentas> ventasPorEmpleado(UUID empresaId, UUID sucursalId);
}
