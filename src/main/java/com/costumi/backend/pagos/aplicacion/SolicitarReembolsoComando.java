package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.TipoConcepto;

import java.math.BigDecimal;
import java.util.UUID;

/** Datos para solicitar un reembolso de una venta o renta (RF-4.5/6.9). */
public record SolicitarReembolsoComando(UUID empresaId, TipoConcepto tipoConcepto, UUID conceptoId,
		UUID solicitanteClienteId, BigDecimal monto, String motivo) {
}
