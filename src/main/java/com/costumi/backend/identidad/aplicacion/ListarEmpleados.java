package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Rol;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: listar el personal de la empresa que el actor puede gestionar (RF-8, G1). */
public interface ListarEmpleados {

	/** Un empleado del listado: datos básicos + sus sucursales asignadas. Nunca expone la contraseña. */
	record EmpleadoDelTenant(UUID id, String email, Rol rol, boolean activo, List<UUID> sucursales) {
	}

	/**
	 * Personal del tenant que el actor ({@code actorRol}) puede gestionar: solo los estrictamente por debajo
	 * suyo en la pirámide (RF-1.3/B3). Así un Encargado ve a los operativos, no al Dueño ni a otros encargados.
	 */
	List<EmpleadoDelTenant> delTenant(UUID empresaId, Rol actorRol);
}
