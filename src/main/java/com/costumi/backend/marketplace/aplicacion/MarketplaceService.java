package com.costumi.backend.marketplace.aplicacion;

import com.costumi.backend.marketplace.dominio.EmpresaEnVitrina;
import com.costumi.backend.marketplace.dominio.MarketplaceReadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Casos de uso del marketplace (solo lectura). */
@Service
class MarketplaceService implements DescubrirEmpresas {

	private final MarketplaceReadRepository marketplace;

	MarketplaceService(MarketplaceReadRepository marketplace) {
		this.marketplace = marketplace;
	}

	@Override
	@Transactional(readOnly = true)
	public List<EmpresaEnVitrina> activas() {
		return marketplace.empresasActivas();
	}
}
