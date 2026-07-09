package com.costumi.backend.configuracion.dominio;

/**
 * Cómo cobra la empresa el recargo por retraso en la devolución (RF-5.2/12.2), a elección del dueño:
 * <ul>
 *   <li>{@link #ACUMULATIVA}: el recargo se cobra <b>por cada día</b> de atraso (monto × días).</li>
 *   <li>{@link #FIJA}: se cobra un <b>monto único</b> si hubo atraso, sin importar cuántos días.</li>
 * </ul>
 * En ambos casos el monto base es {@code recargoPorRetrasoPorDia}. Por defecto: {@link #ACUMULATIVA}.
 */
public enum RecargoPorRetraso {
	ACUMULATIVA,
	FIJA
}
