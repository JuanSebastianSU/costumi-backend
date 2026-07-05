package com.costumi.backend.rentas.dominio;

import java.util.Map;
import java.util.Set;

/**
 * Estados de una Renta y sus transiciones (RF-3.5).
 *
 * <pre>
 *   RESERVADA ──entregar──▶ ACTIVA ──devolver──▶ DEVUELTA ──cerrar──▶ CERRADA
 *       └──cancelar──▶ CANCELADA
 * </pre>
 */
public enum EstadoRenta {

	RESERVADA,
	ACTIVA,
	DEVUELTA,
	CERRADA,
	CANCELADA;

	private static final Map<EstadoRenta, Set<EstadoRenta>> TRANSICIONES = Map.of(
			RESERVADA, Set.of(ACTIVA, CANCELADA),
			ACTIVA, Set.of(DEVUELTA),
			DEVUELTA, Set.of(CERRADA),
			CERRADA, Set.of(),
			CANCELADA, Set.of());

	public boolean puedeTransicionarA(EstadoRenta destino) {
		return TRANSICIONES.get(this).contains(destino);
	}
}
