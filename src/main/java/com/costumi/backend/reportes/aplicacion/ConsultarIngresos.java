package com.costumi.backend.reportes.aplicacion;

import com.costumi.backend.reportes.dominio.IngresoDelDia;
import com.costumi.backend.reportes.dominio.ResumenDeIngresos;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Puerto de entrada: resumen de ingresos de una empresa (RF-9.1), scoped por tenant. */
public interface ConsultarIngresos {

	/**
	 * Ingresos de la empresa (o de una sola sucursal si {@code sucursalId} no es null), opcionalmente
	 * acotados al rango {@code [desde, hasta]} (por fecha del pago; null = sin ese límite).
	 */
	ResumenDeIngresos deEmpresa(UUID empresaId, UUID sucursalId, LocalDate desde, LocalDate hasta);

	/**
	 * Serie de ingreso por día (tendencia del panel). {@code tipoConcepto} null = todos; 'VENTA'/'RENTA'
	 * separan la serie (para graficar ventas vs rentas por día).
	 */
	List<IngresoDelDia> porDia(UUID empresaId, UUID sucursalId, LocalDate desde, LocalDate hasta,
			String tipoConcepto);
}
