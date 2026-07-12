package com.costumi.backend.clientes.aplicacion;

import com.costumi.backend.clientes.dominio.Cliente;
import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Puerto de entrada: lista o busca Clientes de una empresa (RF-7.3), scoped por tenant. */
public interface ConsultarClientes {

	List<Cliente> buscar(UUID empresaId, String texto);

	/**
	 * Página de clientes de la empresa (C3). {@code texto} filtra por nombre/documento/teléfono;
	 * {@code idsFiltro} restringe a esos ids (null = sin restricción; vacío = página vacía).
	 */
	Pagina<Cliente> listar(UUID empresaId, String texto, Collection<UUID> idsFiltro, SolicitudDePagina solicitud);
}
