package com.costumi.backend.reportes.dominio;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Fila de un ranking de DISFRACES (RF-9.1): el disfraz, cuántos se cobraron y cuánto dinero movió.
 *
 * <p>Existe aparte del ranking de prendas porque al cobrar el disfraz se resuelve a sus piezas: sin
 * agrupar por el disfraz de origen, el dueño solo puede ver qué prenda se vende más, nunca qué disfraz.
 * {@code unidades} cuenta disfraces (grupos), no piezas.
 */
public record DisfrazRanking(UUID disfrazId, String nombre, long unidades, BigDecimal monto) {
}
