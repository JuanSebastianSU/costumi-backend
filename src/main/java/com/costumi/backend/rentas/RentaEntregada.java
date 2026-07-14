package com.costumi.backend.rentas;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Evento de dominio (§5.5): se entregó una renta al cliente (pasó a ACTIVA). Otros módulos reaccionan
 * (p. ej. Notificaciones envía la confirmación de renta con la fecha de devolución, RF-11.1) sin que
 * Rentas los conozca.
 */
public record RentaEntregada(UUID empresaId, UUID clienteId, UUID sucursalId, UUID rentaId,
		LocalDate fechaDevolucion) {
}
