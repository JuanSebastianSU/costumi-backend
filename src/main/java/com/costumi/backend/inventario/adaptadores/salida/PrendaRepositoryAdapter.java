package com.costumi.backend.inventario.adaptadores.salida;

import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;
import com.costumi.backend.inventario.dominio.EtiquetasDePrenda;
import com.costumi.backend.inventario.dominio.Prenda;
import com.costumi.backend.inventario.dominio.PrendaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Adaptador de salida: implementa el puerto {@link PrendaRepository} con JPA. */
@Repository
class PrendaRepositoryAdapter implements PrendaRepository {

	private final PrendaJpaRepository jpa;

	PrendaRepositoryAdapter(PrendaJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Prenda guardar(Prenda prenda) {
		return aDominio(jpa.save(aEntidad(prenda)));
	}

	@Override
	public Optional<Prenda> buscarPorId(UUID id) {
		return jpa.findFirstById(id).map(PrendaRepositoryAdapter::aDominio);
	}

	@Override
	public List<Prenda> listarPorEmpresa(UUID empresaId) {
		return jpa.findByEmpresaId(empresaId).stream().map(PrendaRepositoryAdapter::aDominio).toList();
	}

	@Override
	public Pagina<Prenda> listar(UUID empresaId, SolicitudDePagina solicitud) {
		Pageable pageable = PageRequest.of(solicitud.pagina(), solicitud.tamano(),
				Sort.by(Sort.Order.asc("nombre"), Sort.Order.asc("id")));
		Page<PrendaJpaEntity> pagina = jpa.findByEmpresaId(empresaId, pageable);
		return new Pagina<>(pagina.getContent().stream().map(PrendaRepositoryAdapter::aDominio).toList(),
				pagina.getTotalElements(), solicitud.pagina(), solicitud.tamano());
	}

	private static PrendaJpaEntity aEntidad(Prenda p) {
		Set<EtiquetaDePrendaEmbeddable> etiquetas = p.etiquetas().valores().entrySet().stream()
				.map(e -> new EtiquetaDePrendaEmbeddable(e.getKey(), e.getValue()))
				.collect(Collectors.toCollection(LinkedHashSet::new));
		return new PrendaJpaEntity(p.id(), p.empresaId(), p.categoriaId(), p.nombre(), p.tipoArticulo(),
				p.precioRenta(), p.precioVenta(), p.costoAdquisicion(), p.depositoSugerido(), p.valorReposicion(),
				p.valorDano(), etiquetas, p.archivada(), p.fotoUrl());
	}

	private static Prenda aDominio(PrendaJpaEntity e) {
		Map<UUID, UUID> valores = new LinkedHashMap<>();
		e.getEtiquetas().forEach(v -> valores.put(v.getTipoEtiquetaId(), v.getValorEtiquetaId()));
		return Prenda.rehidratar(e.getId(), e.getEmpresaId(), e.getCategoriaId(), e.getNombre(), e.getTipoArticulo(),
				e.getPrecioRenta(), e.getPrecioVenta(), e.getCostoAdquisicion(), e.getDepositoSugerido(),
				e.getValorReposicion(), e.getValorDano(), EtiquetasDePrenda.de(valores), e.isArchivada(),
				e.getFotoUrl());
	}
}
