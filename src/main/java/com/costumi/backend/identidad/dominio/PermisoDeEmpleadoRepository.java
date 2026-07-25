package com.costumi.backend.identidad.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: overrides de capacidades por empleado (Fase B, paso 5), acotados al tenant. */
public interface PermisoDeEmpleadoRepository {

	/** Un override existente: la capacidad y si está concedida. */
	record OverrideDeCapacidad(Capacidad capacidad, boolean concedido) {
	}

	/** Valor del override para (usuario, capacidad), si el dueño lo fijó explícitamente. */
	Optional<Boolean> valor(UUID usuarioId, Capacidad capacidad);

	/** Todos los overrides fijados para el empleado. */
	List<OverrideDeCapacidad> listar(UUID empresaId, UUID usuarioId);

	/** Fija (crea o actualiza) el override de una capacidad. */
	void establecer(UUID empresaId, UUID usuarioId, Capacidad capacidad, boolean concedido);
}
