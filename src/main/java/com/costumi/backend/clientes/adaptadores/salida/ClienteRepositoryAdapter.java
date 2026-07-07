package com.costumi.backend.clientes.adaptadores.salida;

import com.costumi.backend.clientes.dominio.Cliente;
import com.costumi.backend.clientes.dominio.ClienteRepository;
import org.springframework.stereotype.Repository;

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

	private static ClienteJpaEntity aEntidad(Cliente c) {
		return new ClienteJpaEntity(c.id(), c.empresaId(), c.nombre(), c.telefono(), c.email(), c.documento(),
				c.direccion(), c.enListaNegra(), c.deviceToken());
	}

	private static Cliente aDominio(ClienteJpaEntity e) {
		return Cliente.rehidratar(e.getId(), e.getEmpresaId(), e.getNombre(), e.getTelefono(), e.getEmail(),
				e.getDocumento(), e.getDireccion(), e.isEnListaNegra(), e.getDeviceToken());
	}
}
