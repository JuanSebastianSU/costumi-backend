package com.costumi.backend.disfraces.adaptadores.salida;

import com.costumi.backend.disfraces.dominio.CategoriaDeDisfraz;
import com.costumi.backend.disfraces.dominio.CategoriaDeDisfrazRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa {@link CategoriaDeDisfrazRepository} con JPA. */
@Repository
class CategoriaDeDisfrazRepositoryAdapter implements CategoriaDeDisfrazRepository {

	private final CategoriaDeDisfrazJpaRepository jpa;

	CategoriaDeDisfrazRepositoryAdapter(CategoriaDeDisfrazJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public CategoriaDeDisfraz guardar(CategoriaDeDisfraz categoria) {
		return aDominio(jpa.save(aEntidad(categoria)));
	}

	@Override
	public Optional<CategoriaDeDisfraz> buscarPorId(UUID id) {
		return jpa.findFirstById(id).map(CategoriaDeDisfrazRepositoryAdapter::aDominio);
	}

	@Override
	public List<CategoriaDeDisfraz> listarPorEmpresa(UUID empresaId) {
		return jpa.findByEmpresaId(empresaId).stream().map(CategoriaDeDisfrazRepositoryAdapter::aDominio).toList();
	}

	private static CategoriaDeDisfrazJpaEntity aEntidad(CategoriaDeDisfraz c) {
		return new CategoriaDeDisfrazJpaEntity(c.id(), c.empresaId(), c.nombre(), c.archivada());
	}

	private static CategoriaDeDisfraz aDominio(CategoriaDeDisfrazJpaEntity e) {
		return CategoriaDeDisfraz.rehidratar(e.getId(), e.getEmpresaId(), e.getNombre(), e.isArchivada());
	}
}
