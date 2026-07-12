package com.costumi.backend.clientes.adaptadores.salida;

import com.costumi.backend.clientes.dominio.Cliente;
import com.costumi.backend.clientes.dominio.ClienteRepository;
import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link ClienteRepository} con JPA. */
@Repository
class ClienteRepositoryAdapter implements ClienteRepository {

	private final ClienteJpaRepository jpa;

	ClienteRepositoryAdapter(ClienteJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Cliente guardar(Cliente cliente) {
		return aDominio(jpa.save(aEntidad(cliente)));
	}

	@Override
	public Optional<Cliente> buscarPorId(UUID id) {
		return jpa.findFirstById(id).map(ClienteRepositoryAdapter::aDominio);
	}

	@Override
	public List<Cliente> listarPorEmpresa(UUID empresaId) {
		return jpa.findByEmpresaId(empresaId).stream().map(ClienteRepositoryAdapter::aDominio).toList();
	}

	@Override
	public List<Cliente> buscarPorEmpresaYTexto(UUID empresaId, String texto) {
		return jpa.buscar(empresaId, texto).stream().map(ClienteRepositoryAdapter::aDominio).toList();
	}

	@Override
	public Pagina<Cliente> listar(UUID empresaId, String texto, Collection<UUID> idsFiltro,
			SolicitudDePagina solicitud) {
		// Restricción por ids presente pero vacía: ningún cliente califica (p. ej. nadie con pendientes).
		if (idsFiltro != null && idsFiltro.isEmpty()) {
			return new Pagina<>(List.of(), 0, solicitud.pagina(), solicitud.tamano());
		}
		Pageable pageable = PageRequest.of(solicitud.pagina(), solicitud.tamano(),
				Sort.by(Sort.Order.asc("nombre"), Sort.Order.asc("id")));
		boolean hayTexto = texto != null && !texto.isBlank();
		Page<ClienteJpaEntity> pagina;
		if (idsFiltro != null && hayTexto) {
			pagina = jpa.buscarEnIds(empresaId, texto, idsFiltro, pageable);
		} else if (idsFiltro != null) {
			pagina = jpa.findByEmpresaIdAndIdIn(empresaId, idsFiltro, pageable);
		} else if (hayTexto) {
			pagina = jpa.buscarPagina(empresaId, texto, pageable);
		} else {
			pagina = jpa.findByEmpresaId(empresaId, pageable);
		}
		return new Pagina<>(pagina.getContent().stream().map(ClienteRepositoryAdapter::aDominio).toList(),
				pagina.getTotalElements(), solicitud.pagina(), solicitud.tamano());
	}

	@Override
	public Optional<Cliente> buscarPorEmpresaYUsuario(UUID empresaId, UUID usuarioId) {
		return jpa.findByEmpresaIdAndUsuarioId(empresaId, usuarioId).map(ClienteRepositoryAdapter::aDominio);
	}

	@Override
	public List<Cliente> buscarPorUsuario(UUID usuarioId) {
		return jpa.findByUsuarioId(usuarioId).stream().map(ClienteRepositoryAdapter::aDominio).toList();
	}

	private static ClienteJpaEntity aEntidad(Cliente c) {
		return new ClienteJpaEntity(c.id(), c.empresaId(), c.nombre(), c.telefono(), c.email(), c.documento(),
				c.direccion(), c.enListaNegra(), c.deviceToken(), c.usuarioId());
	}

	private static Cliente aDominio(ClienteJpaEntity e) {
		return Cliente.rehidratar(e.getId(), e.getEmpresaId(), e.getNombre(), e.getTelefono(), e.getEmail(),
				e.getDocumento(), e.getDireccion(), e.isEnListaNegra(), e.getDeviceToken(), e.getUsuarioId());
	}
}
