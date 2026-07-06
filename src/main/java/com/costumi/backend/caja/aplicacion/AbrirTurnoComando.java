package com.costumi.backend.caja.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/** Datos para abrir un turno de caja. El empleado sale del token. */
public record AbrirTurnoComando(UUID empresaId, UUID sucursalId, UUID empleadoId, BigDecimal fondoInicial) {
}
