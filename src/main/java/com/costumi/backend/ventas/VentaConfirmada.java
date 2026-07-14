package com.costumi.backend.ventas;

import java.util.UUID;

/**
 * Evento de dominio (§5.5): se confirmó una venta a nombre de un cliente. Otros módulos reaccionan
 * (p. ej. Notificaciones envía el agradecimiento por compra, RF-11.1) sin que Ventas los conozca.
 * Solo se publica cuando la venta tiene cliente (las de mostrador anónimas no notifican).
 */
public record VentaConfirmada(UUID empresaId, UUID clienteId, UUID sucursalId) {
}
