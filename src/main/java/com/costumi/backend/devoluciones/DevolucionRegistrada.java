package com.costumi.backend.devoluciones;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Evento de dominio (§5.5): se registró una devolución que cerró una renta. Lleva la <b>multa</b>
 * automática (RF-5.2). Otros módulos podrán reaccionar (p. ej. registrar la multa como saldo del
 * cliente o notificarla) sin que Devoluciones los conozca.
 */
public record DevolucionRegistrada(UUID empresaId, UUID devolucionId, UUID rentaId, BigDecimal multa) {
}
