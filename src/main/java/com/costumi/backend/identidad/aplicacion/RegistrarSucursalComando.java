package com.costumi.backend.identidad.aplicacion;

import java.util.UUID;

/** Datos de entrada para dar de alta una Sucursal en una Empresa (RF-15.1). */
public record RegistrarSucursalComando(UUID empresaId, String nombre, String direccion, String ubicacionMaps) {
}
