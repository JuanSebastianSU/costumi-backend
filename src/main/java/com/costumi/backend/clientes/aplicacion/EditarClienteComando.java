package com.costumi.backend.clientes.aplicacion;

import java.util.UUID;

/**
 * Datos para editar una ficha de cliente (RF-7). El nombre es obligatorio; el resto es opcional.
 * El correo no se incluye: es inmutable tras crear la ficha.
 */
public record EditarClienteComando(UUID empresaId, UUID clienteId, String nombre, String telefono,
		String documento, String direccion) {
}
