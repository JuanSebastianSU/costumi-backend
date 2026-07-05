package com.costumi.backend.inventario.adaptadores.salida;

import com.costumi.backend.inventario.dominio.GrupoDeStock;
import com.costumi.backend.inventario.dominio.GrupoDeStockRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto {@link GrupoDeStockRepository} con JPA. */
@Repository
class GrupoDeStockRepositoryAdapter implements GrupoDeStockRepository {

	private final GrupoDeStockJpaRepository jpa;

	GrupoDeStockRepositoryAdapter(GrupoDeStockJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public GrupoDeStock guardar(GrupoDeStock grupoDeStock) {
		return aDominio(jpa.save(aEntidad(grupoDeStock)));
	}

	@Override
	public Optional<GrupoDeStock> buscarPorId(UUID id) {
		return jpa.findById(id).map(GrupoDeStockRepositoryAdapter::aDominio);
	}

	@Override
	public List<GrupoDeStock> listarPorPrenda(UUID prendaId) {
		return jpa.findByPrendaId(prendaId).stream().map(GrupoDeStockRepositoryAdapter::aDominio).toList();
	}

	private static GrupoDeStockJpaEntity aEntidad(GrupoDeStock g) {
		return new GrupoDeStockJpaEntity(g.id(), g.empresaId(), g.prendaId(), g.etiqueta(),
				g.disponibles(), g.danadas(), g.enLimpieza(), g.perdidas());
	}

	private static GrupoDeStock aDominio(GrupoDeStockJpaEntity e) {
		return GrupoDeStock.rehidratar(e.getId(), e.getEmpresaId(), e.getPrendaId(), e.getEtiqueta(),
				e.getDisponibles(), e.getDanadas(), e.getEnLimpieza(), e.getPerdidas());
	}
}
