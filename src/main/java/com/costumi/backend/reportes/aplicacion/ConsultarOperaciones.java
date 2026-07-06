package com.costumi.backend.reportes.aplicacion;

import com.costumi.backend.reportes.dominio.IngresosPorMetodo;
import com.costumi.backend.reportes.dominio.RentaVencida;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Puerto de entrada: reportes operativos (rentas vencidas, depósitos activos, ingresos por método), RF-9.1. */
public interface ConsultarOperaciones {

	/** Rentas vencidas de la empresa (opcionalmente acotadas a una sucursal). */
	List<RentaVencida> rentasVencidas(UUID empresaId, UUID sucursalId);

	/** Total de depósitos activos (retenidos) de la empresa (opcionalmente por sucursal). */
	BigDecimal depositosActivos(UUID empresaId, UUID sucursalId);

	/** Ingresos netos por método en un rango de fechas (nulos = sin ese límite), RF-6.10/9.1. */
	IngresosPorMetodo ingresosPorMetodo(UUID empresaId, LocalDate desde, LocalDate hasta, UUID sucursalId);
}
