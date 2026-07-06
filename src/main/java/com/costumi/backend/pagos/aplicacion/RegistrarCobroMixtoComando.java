package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.PorcionDePago;
import com.costumi.backend.pagos.dominio.TipoConcepto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Datos para registrar un cobro mixto (RF-6.7): un cobro repartido en varias porciones (métodos
 * distintos) sobre una misma renta/venta, con el efectivo recibido para calcular el vuelto.
 */
public record RegistrarCobroMixtoComando(UUID empresaId, UUID sucursalId, UUID empleadoId, TipoConcepto tipoConcepto,
		UUID conceptoId, List<PorcionDePago> porciones, BigDecimal efectivoRecibido, String claveIdempotencia) {
}
