package com.costumi.backend.marketplace.aplicacion;

import com.costumi.backend.catalogo.ConsultaDeTaxonomia;
import com.costumi.backend.marketplace.dominio.EmpresaEnVitrina;
import com.costumi.backend.marketplace.dominio.EtiquetaEnVitrina;
import com.costumi.backend.marketplace.dominio.MarketplaceReadRepository;
import com.costumi.backend.marketplace.dominio.PrendaEnVitrina;
import com.costumi.backend.marketplace.dominio.SucursalEnVitrina;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Casos de uso del marketplace (solo lectura). */
@Service
class MarketplaceService implements DescubrirEmpresas {

	private final MarketplaceReadRepository marketplace;
	private final ConsultaDeTaxonomia taxonomia;

	MarketplaceService(MarketplaceReadRepository marketplace, ConsultaDeTaxonomia taxonomia) {
		this.marketplace = marketplace;
		this.taxonomia = taxonomia;
	}

	@Override
	@Transactional(readOnly = true)
	public com.costumi.backend.compartido.Pagina<EmpresaEnVitrina> empresas(String buscar,
			com.costumi.backend.compartido.SolicitudDePagina solicitud) {
		return marketplace.empresas(buscar, solicitud);
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.Optional<EmpresaEnVitrina> empresa(UUID empresaId) {
		return marketplace.empresa(empresaId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<com.costumi.backend.marketplace.dominio.CategoriaEnVitrina> categoriasDe(UUID empresaId) {
		return marketplace.categoriasDe(empresaId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<com.costumi.backend.marketplace.dominio.HorarioEnVitrina> horarioDe(UUID empresaId) {
		return marketplace.horarioDe(empresaId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<com.costumi.backend.marketplace.dominio.DisfrazDestacado> destacados(int limite) {
		return marketplace.destacados(limite);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PrendaEnVitrina> catalogo(UUID empresaId, UUID categoriaId) {
		List<PrendaEnVitrina> prendas = marketplace.catalogoDe(empresaId, categoriaId);
		// Etiquetas de todas las prendas del catálogo, resueltas a nombre en una sola pasada (sin N+1).
		Map<UUID, List<UUID>> valoresPorPrenda = marketplace.valorEtiquetasDeCatalogo(empresaId, categoriaId);
		java.util.Set<UUID> todosLosValores = valoresPorPrenda.values().stream()
				.flatMap(List::stream).collect(java.util.stream.Collectors.toSet());
		Map<UUID, ConsultaDeTaxonomia.EtiquetaConNombre> nombres =
				taxonomia.describirValores(empresaId, todosLosValores);
		return prendas.stream().map(p -> {
			List<EtiquetaEnVitrina> etiquetas = valoresPorPrenda.getOrDefault(p.id(), List.of()).stream()
					.map(nombres::get)
					.filter(java.util.Objects::nonNull)
					.map(e -> new EtiquetaEnVitrina(e.tipoNombre(), e.valorNombre()))
					.toList();
			return new PrendaEnVitrina(p.id(), p.nombre(), p.tipoArticulo(), p.precioRenta(), p.precioVenta(),
					p.categoria(), p.fotoUrl(), etiquetas);
		}).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<SucursalEnVitrina> sucursales(UUID empresaId) {
		return marketplace.sucursalesActivasDe(empresaId);
	}
}
