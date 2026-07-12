package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.TipoConcepto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Datos de una solicitud de reembolso iniciada por el propio cliente (RF-4.5/14.4). El {@code empresaId} lo
 * indica el cliente (a qué tienda le compró/rentó); {@code usuarioId}/{@code email} vienen de su token, para
 * resolver su ficha y verificar que la operación sea suya.
 */
public record SolicitarReembolsoDeClienteComando(UUID empresaId, UUID usuarioId, String email,
		TipoConcepto tipoConcepto, UUID conceptoId, BigDecimal monto, String motivo) {
}
