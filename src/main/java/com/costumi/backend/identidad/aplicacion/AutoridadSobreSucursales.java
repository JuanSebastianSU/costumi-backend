package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.AsignacionDeSucursalesRepository;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.SucursalRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Regla piramidal de sucursales (Fase B, paso 4): el DUEÑO puede asignar cualquier sucursal de su empresa;
 * el ENCARGADO solo <b>entre las suyas</b> (por eso reasignar exige el permiso y las sucursales propias).
 * Se usa tanto al invitar (asignación inicial) como al reasignar.
 */
@Component
class AutoridadSobreSucursales {

	private final AsignacionDeSucursalesRepository asignaciones;
	private final SucursalRepository sucursales;

	AutoridadSobreSucursales(AsignacionDeSucursalesRepository asignaciones, SucursalRepository sucursales) {
		this.asignaciones = asignaciones;
		this.sucursales = sucursales;
	}

	/** Exige que todas las sucursales sean del tenant y que el actor tenga alcance para asignarlas. */
	void exigirAsignables(UUID empresaId, Rol actorRol, UUID actorId, Set<UUID> sucursalIds) {
		for (UUID sucursalId : sucursalIds) {
			boolean delTenant = sucursales.buscarPorId(sucursalId)
					.filter(s -> s.empresaId().equals(empresaId))
					.isPresent();
			if (!delTenant) {
				throw new IllegalArgumentException("La sucursal no existe en esta empresa");
			}
		}
		// El ENCARGADO solo puede asignar entre sus propias sucursales; el DUEÑO, cualquiera del tenant.
		if (actorRol == Rol.ENCARGADO && !sucursalIds.isEmpty()) {
			Set<UUID> propias = new HashSet<>(asignaciones.sucursalesDe(actorId));
			if (!propias.containsAll(sucursalIds)) {
				throw new GestionDeEmpleadoNoPermitida();
			}
		}
	}
}
