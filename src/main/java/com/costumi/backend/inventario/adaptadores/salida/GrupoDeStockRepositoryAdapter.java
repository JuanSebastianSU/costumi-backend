package com.costumi.backend.inventario.adaptadores.salida;

import com.costumi.backend.inventario.dominio.CombinacionDeVariante;
import com.costumi.backend.inventario.dominio.GrupoDeStock;
import com.costumi.backend.inventario.dominio.GrupoDeStockRepository;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
		return jpa.findFirstById(id).map(GrupoDeStockRepositoryAdapter::aDominio);
	}

	@Override
	public List<GrupoDeStock> listarPorPrenda(UUID prendaId) {
		return jpa.findByPrendaId(prendaId).stream().map(GrupoDeStockRepositoryAdapter::aDominio).toList();
	}

	@Override
	public List<GrupoDeStock> listarPorPrendaYSucursal(UUID prendaId, UUID sucursalId) {
		return jpa.findByPrendaIdAndSucursalId(prendaId, sucursalId).stream()
				.map(GrupoDeStockRepositoryAdapter::aDominio).toList();
	}

	@Override
	public List<GrupoDeStock> listarBajoUmbral(UUID empresaId, int umbral) {
		return jpa.findByEmpresaIdAndDisponiblesLessThan(empresaId, umbral).stream()
				.map(GrupoDeStockRepositoryAdapter::aDominio).toList();
	}

	private static GrupoDeStockJpaEntity aEntidad(GrupoDeStock g) {
		Set<ValorDeVarianteEmbeddable> combinacion = g.combinacion().valores().entrySet().stream()
				.map(e -> new ValorDeVarianteEmbeddable(e.getKey(), e.getValue()))
				.collect(Collectors.toCollection(java.util.LinkedHashSet::new));
		return new GrupoDeStockJpaEntity(g.id(), g.empresaId(), g.sucursalId(), g.prendaId(), combinacion,
				g.disponibles(), g.danadas(), g.enLimpieza(), g.perdidas());
	}

	private static GrupoDeStock aDominio(GrupoDeStockJpaEntity e) {
		Map<UUID, UUID> valores = new LinkedHashMap<>();
		e.getCombinacion().forEach(v -> valores.put(v.getTipoEtiquetaId(), v.getValorEtiquetaId()));
		return GrupoDeStock.rehidratar(e.getId(), e.getEmpresaId(), e.getSucursalId(), e.getPrendaId(),
				CombinacionDeVariante.de(valores), e.getDisponibles(), e.getDanadas(), e.getEnLimpieza(),
				e.getPerdidas());
	}
}
