package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.dominio.Sucursal;

import java.util.UUID;

/** DTO de salida de la Sucursal. */
public record SucursalResponse(UUID id, UUID empresaId, String nombre, String direccion, String ubicacionMaps,
		boolean archivada, String descripcion, Double latitud, Double longitud, String fotoUrl) {

	static SucursalResponse desde(Sucursal sucursal) {
		return new SucursalResponse(sucursal.id(), sucursal.empresaId(), sucursal.nombre(), sucursal.direccion(),
				sucursal.ubicacionMaps(), sucursal.archivada(), sucursal.descripcion(), sucursal.latitud(),
				sucursal.longitud(), sucursal.fotoUrl());
	}
}
