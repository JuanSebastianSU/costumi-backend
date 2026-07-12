package com.costumi.backend.marketplace.adaptadores.entrada;

import com.costumi.backend.marketplace.dominio.SucursalEnVitrina;

import java.util.UUID;

/** DTO de salida de una sucursal (punto de retiro) en la vitrina del marketplace (RF-18.5). */
public record SucursalVitrinaResponse(UUID id, String nombre, String direccion) {

	static SucursalVitrinaResponse desde(SucursalEnVitrina s) {
		return new SucursalVitrinaResponse(s.id(), s.nombre(), s.direccion());
	}
}
