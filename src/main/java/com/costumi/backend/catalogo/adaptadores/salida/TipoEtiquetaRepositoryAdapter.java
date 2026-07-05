package com.costumi.backend.catalogo.adaptadores.salida;

import com.costumi.backend.catalogo.dominio.TipoEtiqueta;
import com.costumi.backend.catalogo.dominio.TipoEtiquetaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link TipoEtiquetaRepository} con JPA. */
@Repository
class TipoEtiquetaRepositoryAdapter implements TipoEtiquetaRepository {

	private final TipoEtiquetaJpaRepository jpa;

	TipoEtiquetaRepositoryAdapter(TipoEtiquetaJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public TipoEtiqueta guardar(TipoEtiqueta tipoEtiqueta) {
		return aDominio(jpa.save(aEntidad(tipoEtiqueta)));
	}

	@Override
	public Optional<TipoEtiqueta> buscarPorId(UUID id) {
		return jpa.findById(id).map(TipoEtiquetaRepositoryAdapter::aDominio);
	}

	@Override
	public List<TipoEtiqueta> listarPorEmpresa(UUID empresaId) {
		return jpa.findByEmpresaId(empresaId).stream().map(TipoEtiquetaRepositoryAdapter::aDominio).toList();
	}

	private static TipoEtiquetaJpaEntity aEntidad(TipoEtiqueta t) {
		return new TipoEtiquetaJpaEntity(t.id(), t.empresaId(), t.nombre(), t.defineVariante(),
				t.seleccionablePorCliente(), t.archivada());
	}

	private static TipoEtiqueta aDominio(TipoEtiquetaJpaEntity e) {
		return TipoEtiqueta.rehidratar(e.getId(), e.getEmpresaId(), e.getNombre(), e.isDefineVariante(),
				e.isSeleccionableCliente(), e.isArchivada());
	}
}
