package com.costumi.backend.identidad.aplicacion;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Puerto de entrada: asignar un empleado a una o varias sucursales (RF-1.2/8.1). */
public interface AsignarSucursales {

	/** Reemplaza las sucursales del empleado; valida que empleado y sucursales sean del tenant. */
	void asignar(UUID empresaId, UUID usuarioId, Set<UUID> sucursalIds);

	/** Sucursales asignadas al empleado. */
	List<UUID> sucursalesDe(UUID empresaId, UUID usuarioId);
}
