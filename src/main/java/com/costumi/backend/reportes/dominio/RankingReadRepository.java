package com.costumi.backend.reportes.dominio;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Puerto de salida de rankings (RF-9.1): artículos más vendidos/rentados y ventas por empleado. */
public interface RankingReadRepository {

	/** Prendas más vendidas por unidades (opcionalmente por sucursal), top {@code limite}. */
	List<ArticuloRanking> masVendidos(UUID empresaId, UUID sucursalId, int limite);

	/** Prendas más rentadas por número de rentas (por fecha de retiro/sucursal opcional), top {@code limite}. */
	List<ArticuloRanking> masRentados(UUID empresaId, UUID sucursalId, LocalDate desde, LocalDate hasta, int limite);

	/** Ventas agregadas por empleado (opcionalmente por sucursal). */
	List<EmpleadoVentas> ventasPorEmpleado(UUID empresaId, UUID sucursalId);
}
