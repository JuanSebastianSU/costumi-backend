package com.costumi.backend.clientes.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: favoritos (disfraces guardados) de un usuario del marketplace (C4). */
public interface FavoritoRepository {

	/** Favoritos del usuario, más reciente primero. */
	List<Favorito> listarPorUsuario(UUID usuarioId);

	/** El favorito de ese disfraz para ese usuario, si ya existe (para no duplicar al re-guardar). */
	Optional<Favorito> buscar(UUID usuarioId, UUID disfrazId);

	Favorito guardar(Favorito favorito);

	/** Quita el favorito de ese disfraz para ese usuario (idempotente: si no está, no hace nada). */
	void eliminar(UUID usuarioId, UUID disfrazId);
}
