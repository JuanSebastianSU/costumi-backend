package com.costumi.backend.marketplace.dominio;

import java.util.List;

/** Puerto de salida: modelo de lectura del marketplace. Cruza tenants (solo empresas ACTIVAS). */
public interface MarketplaceReadRepository {

	List<EmpresaEnVitrina> empresasActivas();
}
