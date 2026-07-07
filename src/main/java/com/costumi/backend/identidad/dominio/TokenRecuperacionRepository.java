package com.costumi.backend.identidad.dominio;

import java.util.Optional;

/** Puerto de salida: persistencia de tokens de recuperación de contraseña. */
public interface TokenRecuperacionRepository {

	TokenDeRecuperacion guardar(TokenDeRecuperacion token);

	/** Busca por el hash del token (no por el token en claro, que nunca se guarda). */
	Optional<TokenDeRecuperacion> buscarPorHash(String tokenHash);
}
