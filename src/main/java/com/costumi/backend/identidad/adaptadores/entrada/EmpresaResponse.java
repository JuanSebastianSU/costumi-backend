package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.dominio.Empresa;

import java.time.Instant;
import java.util.UUID;

/** DTO de salida: nunca se expone la entidad de dominio ni la JPA por la API. */
public record EmpresaResponse(UUID id, String nombre, String estado, Instant fechaRegistro) {

	static EmpresaResponse desde(Empresa empresa) {
		return new EmpresaResponse(empresa.id(), empresa.nombre(), empresa.estado().name(), empresa.fechaRegistro());
	}
}
