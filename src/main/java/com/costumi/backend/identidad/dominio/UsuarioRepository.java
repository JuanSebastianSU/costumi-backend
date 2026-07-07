package com.costumi.backend.identidad.dominio;

import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de Usuarios. */
public interface UsuarioRepository {

	Usuario guardar(Usuario usuario);

	Optional<Usuario> buscarPorEmail(String email);

	Optional<Usuario> buscarPorId(UUID id);
}
