package com.costumi.backend.notificaciones.adaptadores.salida;

import com.costumi.backend.notificaciones.dominio.ContactoDeCliente;
import com.costumi.backend.notificaciones.dominio.ContactoDelCliente;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Resuelve el contacto del cliente (teléfono + device_token) leyendo la tabla cliente por SQL, sin
 * acoplar el módulo de notificaciones a las clases internas del módulo de clientes (§5.2).
 */
@Repository
class ContactoDelClienteJdbcAdapter implements ContactoDelCliente {

	// El filtro por empresa_id no es opcional: sin el se podria leer el contacto de un cliente de otra
	// tienda, o notificarlo (§5.4).
	private static final String SQL =
			"select telefono, device_token from cliente where id = :id and empresa_id = :empresaId";

	private final JdbcClient jdbc;

	ContactoDelClienteJdbcAdapter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<ContactoDeCliente> buscar(UUID empresaId, UUID clienteId) {
		return jdbc.sql(SQL).param("id", clienteId).param("empresaId", empresaId)
				.query((rs, n) -> new ContactoDeCliente(rs.getString("telefono"), rs.getString("device_token")))
				.optional();
	}
}
