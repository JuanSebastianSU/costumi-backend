package com.costumi.backend.clientes.aplicacion;

import com.costumi.backend.clientes.dominio.Favorito;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada: "Mis guardados" del cliente del marketplace (C4). Todo se resuelve por el usuario del
 * token (sus favoritos), nunca por un id del request.
 */
public interface GestionarFavoritos {

	List<Favorito> deUsuario(UUID usuarioId);

	/** Guarda (o actualiza el snapshot de) un disfraz favorito del usuario. Idempotente por (usuario, disfraz). */
	Favorito guardar(UUID usuarioId, UUID disfrazId, UUID empresaId, String nombre, String fotoUrl,
			BigDecimal precioRenta, BigDecimal precioVenta);

	void eliminar(UUID usuarioId, UUID disfrazId);
}
