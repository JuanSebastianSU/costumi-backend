package com.costumi.backend.reportes.aplicacion;

import com.costumi.backend.reportes.dominio.ResumenDeGanancia;

import java.time.LocalDate;
import java.util.UUID;

/** Puerto de entrada: consulta la ganancia de la empresa (ingreso − costo, RF-9). */
public interface ConsultarGanancia {

	/**
	 * Ganancia de la empresa (o de una sola sucursal si {@code sucursalId} no es null), opcionalmente
	 * acotada al rango {@code [desde, hasta]} (ingresos por fecha del pago, costo por fecha de la venta).
	 */
	ResumenDeGanancia gananciaDeEmpresa(UUID empresaId, UUID sucursalId, LocalDate desde, LocalDate hasta);
}
