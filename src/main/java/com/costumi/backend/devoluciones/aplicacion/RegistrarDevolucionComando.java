package com.costumi.backend.devoluciones.aplicacion;

import com.costumi.backend.devoluciones.dominio.PiezaRevisada;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Datos para registrar una Devolución: renta, liquidación del depósito y checklist de piezas. */
public record RegistrarDevolucionComando(UUID empresaId, UUID rentaId, BigDecimal deposito, BigDecimal cargoPorDanos,
		BigDecimal cargoPorRetraso, List<PiezaRevisada> piezas) {
}
