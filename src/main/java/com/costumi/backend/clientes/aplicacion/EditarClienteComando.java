package com.costumi.backend.clientes.aplicacion;

import java.util.UUID;

/** Datos para editar una ficha de cliente (RF-7). El nombre es obligatorio; el resto es opcional. */
public record EditarClienteComando(UUID empresaId, UUID clienteId, String nombre, String telefono, String email,
		String documento, String direccion) {
}
