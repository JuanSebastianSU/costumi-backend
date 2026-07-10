package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Rol;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Puerto de entrada: asignar un empleado a una o varias sucursales (RF-1.2/8.1). */
public interface AsignarSucursales {

	/**
	 * Reemplaza las sucursales del empleado; valida que empleado y sucursales sean del tenant y que el actor
	 * ({@code actorRol}) tenga autoridad sobre el empleado (pirámide, RF-1.3/B3).
	 */
	void asignar(UUID empresaId, Rol actorRol, UUID usuarioId, Set<UUID> sucursalIds);

	/** Sucursales asignadas al empleado (mismas restricciones de jerarquía). */
	List<UUID> sucursalesDe(UUID empresaId, Rol actorRol, UUID usuarioId);
}
