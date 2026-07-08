package com.costumi.backend.clientes;

import java.util.UUID;

/**
 * API pública del módulo Clientes para otros módulos (§5.5): resuelve la ficha de cliente de un
 * usuario del marketplace en una empresa. Así el carrito/checkout del CLIENTE (pedidos) puede operar
 * satisfaciendo la relación {@code cliente} de cada tienda sin conocer las clases internas de Clientes.
 */
public interface ResolucionDeClientes {

	/**
	 * Devuelve el id de la ficha de cliente del {@code usuarioId} en la {@code empresaId}; la crea si
	 * no existe (proyección por-tienda de la cuenta del marketplace, RF-14.4/18.5).
	 */
	UUID fichaDeUsuario(UUID empresaId, UUID usuarioId, String email);
}
