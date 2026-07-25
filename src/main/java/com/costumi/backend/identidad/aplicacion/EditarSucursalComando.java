package com.costumi.backend.identidad.aplicacion;

import java.util.UUID;

/** Datos para editar una sucursal (RF-15.1/A7): nombre (obligatorio) y el resto (opcionales). */
public record EditarSucursalComando(UUID empresaId, UUID sucursalId, String nombre, String direccion,
		String ubicacionMaps, String descripcion, Double latitud, Double longitud) {
}
