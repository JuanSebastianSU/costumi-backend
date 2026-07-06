package com.costumi.backend.inventario;

import java.util.UUID;

/**
 * Evento de dominio (§5.5): se ajustó el inventario de un grupo de stock con un motivo (RF-10).
 * Auditoría (u otros) reaccionan para dejar traza, sin que Inventario los conozca. El estado va como
 * texto para no exponer tipos internos de Inventario.
 */
public record StockAjustado(UUID empresaId, UUID prendaId, UUID grupoId, String estado, int delta, String motivo) {
}
