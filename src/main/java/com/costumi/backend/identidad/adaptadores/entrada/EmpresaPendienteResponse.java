package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.EmpresaPendienteVista;

import java.time.Instant;
import java.util.UUID;

/** DTO de salida de una solicitud de Empresa pendiente para el panel del SuperAdmin (RF-15.4). */
public record EmpresaPendienteResponse(UUID id, String nombre, Instant fechaRegistro, boolean vencida,
		String ubicacion, String contacto, UUID solicitanteId) {

	static EmpresaPendienteResponse desde(EmpresaPendienteVista vista) {
		return new EmpresaPendienteResponse(vista.id(), vista.nombre(), vista.fechaRegistro(), vista.vencida(),
				vista.ubicacion(), vista.contacto(), vista.solicitanteId());
	}
}
