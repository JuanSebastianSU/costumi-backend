package com.costumi.backend.identidad.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de Usuarios. */
public interface UsuarioRepository {

	Usuario guardar(Usuario usuario);

	Optional<Usuario> buscarPorEmail(String email);

	Optional<Usuario> buscarPorId(UUID id);

	/** Usuarios (personal) de una empresa (tenant): todos los que llevan ese {@code empresa_id} (RF-8). */
	List<Usuario> listarPorEmpresa(UUID empresaId);
}
