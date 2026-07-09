package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link UsuarioRepository} con JPA. */
@Repository
class UsuarioRepositoryAdapter implements UsuarioRepository {

	private final UsuarioJpaRepository jpa;

	UsuarioRepositoryAdapter(UsuarioJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Usuario guardar(Usuario usuario) {
		return aDominio(jpa.save(aEntidad(usuario)));
	}

	@Override
	public Optional<Usuario> buscarPorEmail(String email) {
		return jpa.findByEmail(email).map(UsuarioRepositoryAdapter::aDominio);
	}

	@Override
	public Optional<Usuario> buscarPorId(UUID id) {
		return jpa.findFirstById(id).map(UsuarioRepositoryAdapter::aDominio);
	}

	private static UsuarioJpaEntity aEntidad(Usuario u) {
		return new UsuarioJpaEntity(u.id(), u.empresaId(), u.email(), u.passwordHash(), u.rol(), u.activo());
	}

	private static Usuario aDominio(UsuarioJpaEntity e) {
		return Usuario.rehidratar(e.getId(), e.getEmpresaId(), e.getEmail(), e.getPasswordHash(), e.getRol(),
				e.isActivo());
	}
}
