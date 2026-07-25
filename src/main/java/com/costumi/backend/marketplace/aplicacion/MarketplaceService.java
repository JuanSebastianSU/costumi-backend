package com.costumi.backend.marketplace.aplicacion;

import com.costumi.backend.marketplace.dominio.EmpresaEnVitrina;
import com.costumi.backend.marketplace.dominio.MarketplaceReadRepository;
import com.costumi.backend.marketplace.dominio.PrendaEnVitrina;
import com.costumi.backend.marketplace.dominio.SucursalEnVitrina;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Casos de uso del marketplace (solo lectura). */
@Service
class MarketplaceService implements DescubrirEmpresas {

	private final MarketplaceReadRepository marketplace;

	MarketplaceService(MarketplaceReadRepository marketplace) {
		this.marketplace = marketplace;
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
		return marketplace.catalogoDe(empresaId, categoriaId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SucursalEnVitrina> sucursales(UUID empresaId) {
		return marketplace.sucursalesActivasDe(empresaId);
	}
}
