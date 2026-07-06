package com.costumi.backend.pagos.aplicacion;

import com.costumi.backend.pagos.dominio.Pago;

import java.math.BigDecimal;
import java.util.List;

/** Resultado de un cobro mixto: los pagos generados (uno por método), el total y el vuelto (RF-6.7). */
public record ResultadoCobroMixto(List<Pago> pagos, BigDecimal total, BigDecimal vuelto) {
}
