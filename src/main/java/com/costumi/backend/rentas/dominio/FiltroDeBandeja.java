package com.costumi.backend.rentas.dominio;

import java.util.List;
import java.util.Locale;

/**
 * Traduce la «bandeja» que pide la app (POR_ENTREGAR / ACTIVAS / VENCIDAS / CERRADAS) a criterios que la
 * consulta entiende, para que el JPQL <b>no conozca</b> el enum de bandejas (RF-3.5). Con {@code hoy} = fecha
 * del servidor:
 * <ul>
 *   <li><b>POR_ENTREGAR</b>: RESERVADA.
 *   <li><b>ACTIVAS</b>: ACTIVA con {@code fechaDevolucion >= hoy} + todas las DEVUELTA (faltan por cerrar).
 *   <li><b>VENCIDAS</b>: ACTIVA con {@code fechaDevolucion < hoy}.
 *   <li><b>CERRADAS</b>: CERRADA + CANCELADA (historial).
 * </ul>
 * Un filtro nulo/desconocido = <b>todas</b> ({@code estados == null}, sin condición de fecha).
 */
public record FiltroDeBandeja(List<EstadoRenta> estados, boolean soloVencidas, boolean soloActivasEnFecha) {

	// "Todas" pasa TODOS los estados (no null) para no toparse con el manejo de colección nula en `in`.
	private static final FiltroDeBandeja TODAS = new FiltroDeBandeja(List.of(EstadoRenta.values()), false, false);

	public static FiltroDeBandeja desde(String bandeja) {
		if (bandeja == null || bandeja.isBlank()) {
			return TODAS;
		}
		return switch (bandeja.trim().toUpperCase(Locale.ROOT)) {
			case "POR_ENTREGAR" -> new FiltroDeBandeja(List.of(EstadoRenta.RESERVADA), false, false);
			case "ACTIVAS" -> new FiltroDeBandeja(List.of(EstadoRenta.ACTIVA, EstadoRenta.DEVUELTA), false, true);
			case "VENCIDAS" -> new FiltroDeBandeja(List.of(EstadoRenta.ACTIVA), true, false);
			case "CERRADAS" -> new FiltroDeBandeja(List.of(EstadoRenta.CERRADA, EstadoRenta.CANCELADA), false, false);
			default -> TODAS; // desconocido = todas (no rompe la lista)
		};
	}
}
