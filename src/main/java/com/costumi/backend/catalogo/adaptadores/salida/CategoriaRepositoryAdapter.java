package com.costumi.backend.catalogo.adaptadores.salida;

import com.costumi.backend.catalogo.dominio.Categoria;
import com.costumi.backend.catalogo.dominio.CategoriaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link CategoriaRepository} con JPA. */
@Repository
class CategoriaRepositoryAdapter implements CategoriaRepository {

	private final CategoriaJpaRepository jpa;

	CategoriaRepositoryAdapter(CategoriaJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Categoria guardar(Categoria categoria) {
		return aDominio(jpa.save(aEntidad(categoria)));
	}

	@Override
	public Optional<Categoria> buscarPorId(UUID id) {
		return jpa.findFirstById(id).map(CategoriaRepositoryAdapter::aDominio);
	}

	@Override
	public List<Categoria> listarPorEmpresa(UUID empresaId) {
		return jpa.findByEmpresaId(empresaId).stream().map(CategoriaRepositoryAdapter::aDominio).toList();
	}

	private static CategoriaJpaEntity aEntidad(Categoria c) {
		return new CategoriaJpaEntity(c.id(), c.empresaId(), c.nombre(), c.archivada());
	}

	private static Categoria aDominio(CategoriaJpaEntity e) {
		return Categoria.rehidratar(e.getId(), e.getEmpresaId(), e.getNombre(), e.isArchivada());
	}
}
