package com.costumi.backend.devoluciones.aplicacion;

import com.costumi.backend.devoluciones.dominio.PiezaRevisada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Datos para registrar una Devolución: renta, liquidación del depósito y checklist de piezas.
 * {@code fechaDevolucionReal} es la fecha real de entrega (para el recargo por retraso, RF-5.2);
 * {@code cargoPorRetraso} es opcional: si viene nulo se <b>deriva</b> del recargo configurado por día
 * multiplicado por los días de atraso (RF-12.2); si viene, es un override manual.
 */
public record RegistrarDevolucionComando(UUID empresaId, UUID rentaId, BigDecimal deposito, BigDecimal cargoPorDanos,
		BigDecimal cargoPorRetraso, LocalDate fechaDevolucionReal, List<PiezaRevisada> piezas) {
}
