package com.costumi.backend.identidad.dominio;

import java.util.Map;
import java.util.Set;

/**
 * Estados de una Empresa (tenant) y sus transiciones válidas (RF-15.3).
 *
 * <pre>
 *   PENDIENTE ──aprobar──▶ ACTIVA ──suspender──▶ SUSPENDIDA
 *      │                     ▲                        │
 *      └──rechazar──▶ RECHAZADA                       └──reactivar──▶ ACTIVA
 * </pre>
 *
 * Una empresa PENDIENTE no puede operar (RF-15.4); eso lo hacen respetar las capas
 * superiores, aquí solo se garantiza que los cambios de estado sean legales.
 */
public enum EstadoEmpresa {

	PENDIENTE,
	ACTIVA,
	SUSPENDIDA,
	RECHAZADA;

	private static final Map<EstadoEmpresa, Set<EstadoEmpresa>> TRANSICIONES = Map.of(
			PENDIENTE, Set.of(ACTIVA, RECHAZADA),
			ACTIVA, Set.of(SUSPENDIDA),
			SUSPENDIDA, Set.of(ACTIVA),
			RECHAZADA, Set.of());

	public boolean puedeTransicionarA(EstadoEmpresa destino) {
		return TRANSICIONES.get(this).contains(destino);
	}

	/** Solo una empresa ACTIVA puede operar y ser visible en el marketplace (RF-15.4, RF-15.6). */
	public boolean esOperativa() {
		return this == ACTIVA;
	}
}
