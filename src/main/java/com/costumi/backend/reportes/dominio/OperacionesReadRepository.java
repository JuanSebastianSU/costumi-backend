package com.costumi.backend.reportes.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Puerto de salida de reportes operativos (RF-9.1): rentas vencidas y depósitos activos, por tenant. */
public interface OperacionesReadRepository {

	/** Rentas ACTIVAS cuya fecha de devolución es anterior a {@code hoy}. {@code sucursalId} nulo = todas. */
	List<RentaVencida> rentasVencidas(UUID empresaId, UUID sucursalId, LocalDate hoy);

	/** Suma de depósitos retenidos de rentas aún no cerradas/canceladas. {@code sucursalId} nulo = todas. */
	BigDecimal depositosActivos(UUID empresaId, UUID sucursalId);

	/**
	 * Ingresos netos por método en un rango de fechas (por {@code pago.fecha}). {@code desde}/{@code hasta}
	 * y {@code sucursalId} nulos = sin ese límite (RF-6.10/9.1).
	 */
	IngresosPorMetodo ingresosPorMetodo(UUID empresaId, LocalDate desde, LocalDate hasta, UUID sucursalId);
}
