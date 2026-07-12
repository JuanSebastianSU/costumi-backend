package com.costumi.backend.clientes.adaptadores.entrada;

import com.costumi.backend.clientes.dominio.Cliente;

import java.util.UUID;

/** DTO de salida del Cliente. */
public record ClienteResponse(UUID id, UUID empresaId, String nombre, String telefono, String email,
		String documento, String direccion, boolean enListaNegra, boolean archivada) {

	static ClienteResponse desde(Cliente c) {
		return new ClienteResponse(c.id(), c.empresaId(), c.nombre(), c.telefono(), c.email(), c.documento(),
				c.direccion(), c.enListaNegra(), c.archivada());
	}
}
