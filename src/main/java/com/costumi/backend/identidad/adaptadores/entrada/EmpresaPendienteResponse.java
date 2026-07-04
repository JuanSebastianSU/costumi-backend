package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.EmpresaPendienteVista;

import java.time.Instant;
import java.util.UUID;

/** DTO de salida de una solicitud de Empresa pendiente (RF-15.4). */
public record EmpresaPendienteResponse(UUID id, String nombre, Instant fechaRegistro, boolean vencida) {

	static EmpresaPendienteResponse desde(EmpresaPendienteVista vista) {
		return new EmpresaPendienteResponse(vista.id(), vista.nombre(), vista.fechaRegistro(), vista.vencida());
	}
}
