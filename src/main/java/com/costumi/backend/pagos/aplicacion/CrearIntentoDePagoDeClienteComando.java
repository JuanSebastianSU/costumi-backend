package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.TipoConcepto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Datos de un intento de pago en línea iniciado por el propio cliente (RF-6.11/14.4). El {@code empresaId} lo
 * indica el cliente (a qué tienda le compró/rentó); {@code usuarioId}/{@code email} vienen de su token, para
 * resolver su ficha y verificar que la operación sea suya. El {@code monto} se valida contra el total pendiente.
 */
public record CrearIntentoDePagoDeClienteComando(UUID empresaId, UUID usuarioId, String email, UUID sucursalId,
		TipoConcepto tipoConcepto, UUID conceptoId, BigDecimal monto, String moneda) {
}
