package com.costumi.backend.caja.aplicacion;

import com.costumi.backend.caja.dominio.MetodoDePago;
import com.costumi.backend.caja.dominio.TipoMovimiento;

import java.math.BigDecimal;
import java.util.UUID;

/** Datos para registrar un movimiento en un turno de caja. */
public record RegistrarMovimientoComando(UUID empresaId, UUID turnoId, TipoMovimiento tipo, String concepto,
		BigDecimal monto, MetodoDePago metodo) {
}
