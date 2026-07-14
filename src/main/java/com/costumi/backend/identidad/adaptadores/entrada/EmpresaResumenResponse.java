package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.EmpresaVista;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de salida del listado de Empresas del SuperAdmin: incluye el estado para decidir la acción
 * disponible (suspender una ACTIVA, reactivar una SUSPENDIDA). Nunca se expone el dominio.
 */
public record EmpresaResumenResponse(UUID id, String nombre, String estado, Instant fechaRegistro,
		String ubicacion, String contacto) {

	static EmpresaResumenResponse desde(EmpresaVista vista) {
		return new EmpresaResumenResponse(vista.id(), vista.nombre(), vista.estado().name(),
				vista.fechaRegistro(), vista.ubicacion(), vista.contacto());
	}
}
