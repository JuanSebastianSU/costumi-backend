package com.costumi.backend.marketplace.dominio;

import java.util.List;
import java.util.UUID;

/** Puerto de salida: modelo de lectura del marketplace. Cruza tenants (solo empresas ACTIVAS). */
public interface MarketplaceReadRepository {

	List<EmpresaEnVitrina> empresasActivas();

	/** Empresas ACTIVAS cuyo nombre contiene el texto (RF-18.1). Texto vacío = todas. */
	List<EmpresaEnVitrina> buscarEmpresas(String texto);

	/** Catálogo público (prendas no archivadas) de una empresa ACTIVA. Vacío si no está activa. */
	List<PrendaEnVitrina> catalogoDe(UUID empresaId, UUID categoriaId);

	/**
	 * Sucursales ACTIVAS (puntos de retiro) de una empresa ACTIVA, para que el cliente elija dónde
	 * retirar (RF-18.5). Vacío si la empresa no existe o no está activa.
	 */
	List<SucursalEnVitrina> sucursalesActivasDe(UUID empresaId);
}
