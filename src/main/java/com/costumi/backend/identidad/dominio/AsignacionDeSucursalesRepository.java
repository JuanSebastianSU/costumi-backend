package com.costumi.backend.identidad.dominio;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Puerto de salida: sucursales asignadas a un empleado (RF-1.2), acotado al tenant. */
public interface AsignacionDeSucursalesRepository {

	/** Reemplaza el conjunto de sucursales del empleado por el indicado. */
	void reemplazar(UUID empresaId, UUID usuarioId, Set<UUID> sucursalIds);

	/** Sucursales asignadas al empleado. */
	List<UUID> sucursalesDe(UUID usuarioId);
}
