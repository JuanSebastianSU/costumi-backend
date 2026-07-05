package com.costumi.backend.clientes.aplicacion;

import java.util.UUID;

/** Datos para dar de alta un Cliente en la empresa del usuario autenticado (RF-7.1). */
public record CrearClienteComando(UUID empresaId, String nombre, String telefono, String email, String documento,
		String direccion) {
}
