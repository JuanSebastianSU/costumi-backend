package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.MetodoPago;
import com.costumi.backend.pagos.dominio.TipoConcepto;

import java.math.BigDecimal;
import java.util.UUID;

/** Datos para registrar un Pago ligado a una renta o venta (RF-6.1). */
public record RegistrarPagoComando(UUID empresaId, UUID sucursalId, UUID empleadoId, TipoConcepto tipoConcepto,
		UUID conceptoId, BigDecimal monto, MetodoPago metodo, String referencia, String claveIdempotencia) {
}
