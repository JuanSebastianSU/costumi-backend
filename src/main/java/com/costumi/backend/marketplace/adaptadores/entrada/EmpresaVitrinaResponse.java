package com.costumi.backend.marketplace.adaptadores.entrada;

import com.costumi.backend.marketplace.dominio.EmpresaEnVitrina;

import java.util.UUID;

/** DTO de salida de una empresa en la vitrina del marketplace. */
public record EmpresaVitrinaResponse(UUID id, String nombre, String logoUrl, String portadaUrl, String ciudad,
		String descripcion, long disfracesCount) {

	static EmpresaVitrinaResponse desde(EmpresaEnVitrina e) {
		return new EmpresaVitrinaResponse(e.id(), e.nombre(), e.logoUrl(), e.portadaUrl(), e.ciudad(),
				e.descripcion(), e.disfracesCount());
	}
}
