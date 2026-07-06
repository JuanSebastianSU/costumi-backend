package com.costumi.backend.reportes.dominio;

import java.math.BigDecimal;

/**
 * Fila del desglose de ventas por valor de una dimensión de etiqueta (RF-9.1): p. ej. por color
 * (Rojo/Azul…) o por talla (S/M/L…), con unidades y monto vendidos.
 */
public record ValorEtiquetaRanking(String valor, long unidades, BigDecimal monto) {
}
