package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.EstadoEmpresa;

import java.time.Instant;
import java.util.UUID;

/**
 * Vista de lectura de una Empresa para el panel del SuperAdmin (RF-15.3): incluye el estado del ciclo
 * de vida, para poder suspender una ACTIVA o reactivar una SUSPENDIDA.
 */
public record EmpresaVista(UUID id, String nombre, EstadoEmpresa estado, Instant fechaRegistro,
		String ubicacion, String contacto) {
}
