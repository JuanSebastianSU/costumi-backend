package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.Membresia;
import com.costumi.backend.identidad.dominio.MembresiaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa {@link MembresiaRepository} con JPA (upsert por usuario+empresa). */
@Repository
class MembresiaRepositoryAdapter implements MembresiaRepository {

	private final MembresiaJpaRepository jpa;

	MembresiaRepositoryAdapter(MembresiaJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public List<Membresia> deUsuario(UUID usuarioId) {
		return jpa.findByUsuarioId(usuarioId).stream().map(MembresiaRepositoryAdapter::aDominio).toList();
	}

	@Override
	public Optional<Membresia> buscar(UUID usuarioId, UUID empresaId) {
		return jpa.findByUsuarioIdAndEmpresaId(usuarioId, empresaId).map(MembresiaRepositoryAdapter::aDominio);
	}

	@Override
	public Membresia guardar(Membresia m) {
		// Upsert por (usuario, empresa): reusa el id existente para no violar el único ni duplicar.
		UUID id = jpa.findByUsuarioIdAndEmpresaId(m.usuarioId(), m.empresaId())
				.map(MembresiaJpaEntity::getId).orElse(m.id());
		return aDominio(jpa.save(new MembresiaJpaEntity(id, m.usuarioId(), m.empresaId(), m.rol(), m.estado())));
	}

	private static Membresia aDominio(MembresiaJpaEntity e) {
		return new Membresia(e.getId(), e.getUsuarioId(), e.getEmpresaId(), e.getRol(), e.getEstado());
	}
}
