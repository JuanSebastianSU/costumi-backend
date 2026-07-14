package com.costumi.backend.notificaciones.aplicacion;

import com.costumi.backend.identidad.ConsultaDeSucursales;

import java.util.Map;
import java.util.UUID;

/** Resuelve las variables {@code {direccion}} y {@code {maps}} de una sucursal para las plantillas. */
final class VariablesDeSucursal {

	private VariablesDeSucursal() {
	}

	static void agregar(Map<String, String> variables, ConsultaDeSucursales sucursales, UUID empresaId,
			UUID sucursalId) {
		ConsultaDeSucursales.UbicacionDeSucursal ubi = sucursales.ubicacion(empresaId, sucursalId).orElse(null);
		variables.put("direccion", ubi == null || ubi.direccion() == null ? "" : ubi.direccion());
		variables.put("maps", ubi == null || ubi.ubicacionMaps() == null ? "" : ubi.ubicacionMaps());
	}
}
