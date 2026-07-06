package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.MetodoPago;
import com.costumi.backend.pagos.dominio.TipoConcepto;
import com.costumi.backend.pagos.dominio.TipoPago;

import java.math.BigDecimal;
import java.util.UUID;

/** Datos para registrar un Pago ligado a una renta o venta (RF-6.1). {@code tipoPago} nulo = COBRO. */
public record RegistrarPagoComando(UUID empresaId, UUID sucursalId, UUID empleadoId, TipoConcepto tipoConcepto,
		UUID conceptoId, BigDecimal monto, TipoPago tipoPago, MetodoPago metodo, String referencia,
		String claveIdempotencia) {
}
