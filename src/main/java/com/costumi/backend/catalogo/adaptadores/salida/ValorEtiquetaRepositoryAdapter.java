package com.costumi.backend.catalogo.adaptadores.salida;

import com.costumi.backend.catalogo.dominio.ValorEtiqueta;
import com.costumi.backend.catalogo.dominio.ValorEtiquetaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link ValorEtiquetaRepository} con JPA. */
@Repository
class ValorEtiquetaRepositoryAdapter implements ValorEtiquetaRepository {

	private final ValorEtiquetaJpaRepository jpa;

	ValorEtiquetaRepositoryAdapter(ValorEtiquetaJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public ValorEtiqueta guardar(ValorEtiqueta valorEtiqueta) {
		return aDominio(jpa.save(aEntidad(valorEtiqueta)));
	}

	@Override
	public Optional<ValorEtiqueta> buscarPorId(UUID id) {
		return jpa.findById(id).map(ValorEtiquetaRepositoryAdapter::aDominio);
	}

	@Override
	public List<ValorEtiqueta> listarPorTipo(UUID tipoEtiquetaId) {
		return jpa.findByTipoEtiquetaId(tipoEtiquetaId).stream().map(ValorEtiquetaRepositoryAdapter::aDominio).toList();
	}

	private static ValorEtiquetaJpaEntity aEntidad(ValorEtiqueta v) {
		return new ValorEtiquetaJpaEntity(v.id(), v.empresaId(), v.tipoEtiquetaId(), v.valor(), v.archivada());
	}

	private static ValorEtiqueta aDominio(ValorEtiquetaJpaEntity e) {
		return ValorEtiqueta.rehidratar(e.getId(), e.getEmpresaId(), e.getTipoEtiquetaId(), e.getValor(), e.isArchivada());
	}
}
