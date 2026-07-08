package com.costumi.backend.identidad.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: overrides de permisos por empleado (RF-1.5), acotados al tenant. */
public interface PermisoDeEmpleadoRepository {

	/** Un override existente: el permiso y si está concedido. */
	record OverrideDePermiso(Permiso permiso, boolean concedido) {
	}

	/** Valor del override para (usuario, sección, acción), si el dueño lo fijó explícitamente. */
	Optional<Boolean> valor(UUID usuarioId, Seccion seccion, AccionDePermiso accion);

	/** Todos los overrides fijados para el empleado. */
	List<OverrideDePermiso> listar(UUID empresaId, UUID usuarioId);

	/** Fija (crea o actualiza) el override de una casilla. */
	void establecer(UUID empresaId, UUID usuarioId, Seccion seccion, AccionDePermiso accion, boolean concedido);
}
