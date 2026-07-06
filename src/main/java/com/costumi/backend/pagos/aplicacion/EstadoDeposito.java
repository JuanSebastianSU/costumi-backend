package com.costumi.backend.pagos.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Estado de la garantía retenida de una operación (RF-6.2/6.8): cuánto se retuvo, cuánto se devolvió
 * y cuánto sigue activo (retenido − devuelto). El depósito no es ingreso; se rastrea aquí aparte.
 */
public record EstadoDeposito(UUID conceptoId, BigDecimal retenido, BigDecimal devuelto, BigDecimal activo) {
}
