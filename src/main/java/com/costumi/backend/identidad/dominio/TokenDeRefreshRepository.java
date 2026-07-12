package com.costumi.backend.identidad.dominio;

import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de los tokens de refresco emitidos (C2). Nunca se expone por la API. */
public interface TokenDeRefreshRepository {

	TokenDeRefresh guardar(TokenDeRefresh token);

	Optional<TokenDeRefresh> buscarPorJti(UUID jti);

	/** Anula toda la familia (logout o reuso detectado): ningún refresco de esa cadena vuelve a servir. */
	void revocarFamilia(UUID familiaId);
}
