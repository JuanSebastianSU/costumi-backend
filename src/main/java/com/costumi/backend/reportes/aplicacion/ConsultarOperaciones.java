package com.costumi.backend.reportes.aplicacion;

import com.costumi.backend.reportes.dominio.RentaVencida;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Puerto de entrada: reportes operativos (rentas vencidas, depósitos activos), RF-9.1. */
public interface ConsultarOperaciones {

	/** Rentas vencidas de la empresa (opcionalmente acotadas a una sucursal). */
	List<RentaVencida> rentasVencidas(UUID empresaId, UUID sucursalId);

	/** Total de depósitos activos (retenidos) de la empresa (opcionalmente por sucursal). */
	BigDecimal depositosActivos(UUID empresaId, UUID sucursalId);
}
