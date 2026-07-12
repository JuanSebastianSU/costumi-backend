package com.costumi.backend.clientes.dominio;

import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;

import java.util.Collection;
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

	/**
	 * Página de clientes de la empresa, en orden estable por nombre (C3). {@code texto} filtra por
	 * nombre/documento/teléfono (null/vacío = sin filtro); {@code idsFiltro} restringe a esos ids
	 * (null = sin restricción; vacío = página vacía), para vistas como "con pendientes".
	 */
	Pagina<Cliente> listar(UUID empresaId, String texto, Collection<UUID> idsFiltro, SolicitudDePagina solicitud);

	/** Ficha de un usuario del marketplace en la empresa, si existe (RF-14.4). */
	Optional<Cliente> buscarPorEmpresaYUsuario(UUID empresaId, UUID usuarioId);

	/** Todas las fichas de un usuario del marketplace (en cualquier empresa), para su historial (RF-14.4). */
	List<Cliente> buscarPorUsuario(UUID usuarioId);
}
