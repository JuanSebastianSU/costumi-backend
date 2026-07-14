package com.costumi.backend.identidad.aplicacion;

import java.util.UUID;

/** Datos para editar una sucursal (RF-15.1): nombre (obligatorio) y dirección (opcional). */
public record EditarSucursalComando(UUID empresaId, UUID sucursalId, String nombre, String direccion,
		String ubicacionMaps) {
}
