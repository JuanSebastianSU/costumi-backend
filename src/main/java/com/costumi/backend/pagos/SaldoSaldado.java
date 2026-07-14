package com.costumi.backend.pagos;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Evento de dominio (§5.5): un cliente terminó de saldar la deuda de una renta que tenía multa
 * (pagó el importe de la renta + la multa). Otros módulos reaccionan (p. ej. Notificaciones avisa
 * "ya no adeudas nada", RF-11.1) sin que Pagos los conozca. {@code monto} es la multa saldada.
 */
public record SaldoSaldado(UUID empresaId, UUID clienteId, BigDecimal monto) {
}
