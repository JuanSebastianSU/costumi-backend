package com.costumi.backend.pedidos.dominio;

import java.util.List;
import java.util.UUID;

/** Puerto de salida: los carritos PENDIENTES de un usuario en TODAS las tiendas (modelo de lectura). */
public interface CarritosAbiertosReadRepository {

	/**
	 * Carritos con al menos una línea, del usuario (por todas sus fichas de cliente), en cualquier tienda.
	 * Se filtra por el <b>usuario del token</b>, no por empresa: es una consulta del cliente sobre lo suyo,
	 * igual que su historial.
	 */
	List<CarritoAbierto> deUsuario(UUID usuarioId);
}
