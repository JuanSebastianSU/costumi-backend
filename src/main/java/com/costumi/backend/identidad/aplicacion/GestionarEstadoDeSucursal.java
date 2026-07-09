package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Sucursal;

import java.util.UUID;

/** Puerto de entrada: archivar/reactivar una sucursal (RF-15.1). */
public interface GestionarEstadoDeSucursal {

	/** Archiva la sucursal; falla con {@link SucursalConDependencias} si tiene stock o rentas vigentes. */
	Sucursal archivar(UUID empresaId, UUID sucursalId);

	/** Reactiva una sucursal archivada. */
	Sucursal activar(UUID empresaId, UUID sucursalId);
}
