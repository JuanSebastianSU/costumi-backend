package com.costumi.backend.clientes.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de Clientes (scoped por tenant). */
public interface ClienteRepository {

	Cliente guardar(Cliente cliente);

	Optional<Cliente> buscarPorId(UUID id);

	List<Cliente> listarPorEmpresa(UUID empresaId);

	/** Búsqueda por texto en nombre/documento/teléfono dentro de la empresa (RF-7.3). */
	List<Cliente> buscarPorEmpresaYTexto(UUID empresaId, String texto);

	/** Ficha de un usuario del marketplace en la empresa, si existe (RF-14.4). */
	Optional<Cliente> buscarPorEmpresaYUsuario(UUID empresaId, UUID usuarioId);
}
