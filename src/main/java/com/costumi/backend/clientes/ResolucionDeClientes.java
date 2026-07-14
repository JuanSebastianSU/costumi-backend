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

	/**
	 * Ficha del {@code usuarioId} en la {@code empresaId} <b>si ya existe</b> (no la crea): para verificar que
	 * un cliente solo opere sobre sus propias operaciones sin dejar fichas vacías (RF-14.4).
	 */
	java.util.Optional<UUID> fichaDeUsuarioSiExiste(UUID empresaId, UUID usuarioId);

	/**
	 * ¿El cliente existe y pertenece a la empresa (tenant)? Validación de referencia cruzada (§5.4, SEC-2):
	 * la usan Rentas/Ventas/Pedidos para no anclar una operación a un cliente inexistente o de otra empresa.
	 * Filtra por {@code empresaId} explícito (correcto aun con el filtro multi-tenant apagado).
	 */
	boolean existe(UUID empresaId, UUID clienteId);

	/**
	 * ¿El cliente (de la empresa) está en <b>lista negra</b>? (RF-7.4). La usa Rentas para impedir que un
	 * cliente bloqueado inicie una nueva renta. {@code false} si el cliente no existe o no es de la empresa.
	 */
	boolean estaEnListaNegra(UUID empresaId, UUID clienteId);

	/**
	 * ¿La ficha del cliente (de la empresa) está <b>archivada</b>? (R-E). La usan Rentas/Ventas para que el
	 * personal no opere sobre una ficha retirada (el auto-checkout del marketplace NO la consulta). {@code false}
	 * si el cliente no existe o no es de la empresa.
	 */
	boolean estaArchivado(UUID empresaId, UUID clienteId);

	/**
	 * Nombre de la ficha de cliente (de la empresa), si existe. Lo usa Auditoría para dejar una traza
	 * legible ("Devolución de 'Ana Torres'") en vez de un id opaco (RF-0.5). Vacío si no existe o no es
	 * de la empresa.
	 */
	java.util.Optional<String> nombreDeCliente(UUID empresaId, UUID clienteId);
}
