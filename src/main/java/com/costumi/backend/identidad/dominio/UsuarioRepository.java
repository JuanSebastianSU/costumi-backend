package com.costumi.backend.identidad.dominio;

import java.util.Optional;

/** Puerto de salida: persistencia de Usuarios. */
public interface UsuarioRepository {

	Usuario guardar(Usuario usuario);

	Optional<Usuario> buscarPorEmail(String email);
}
