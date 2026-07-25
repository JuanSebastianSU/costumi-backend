package com.costumi.backend.marketplace.dominio;

import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: modelo de lectura del marketplace. Cruza tenants (solo empresas ACTIVAS). */
public interface MarketplaceReadRepository {

	/**
	 * Página de empresas ACTIVAS (con al menos un punto de retiro), ordenadas por nombre. {@code texto}
	 * (opcional) filtra por nombre. Paginado porque el marketplace crece sin techo (RF-18.1).
	 */
	Pagina<EmpresaEnVitrina> empresas(String texto, SolicitudDePagina solicitud);

	/** Detalle público de una tienda ACTIVA (para la cabecera de su vitrina). Vacío si no existe/no activa. */
	Optional<EmpresaEnVitrina> empresa(UUID empresaId);

	/** Facetas: categorías presentes en el catálogo público de la tienda (RF-18.1). */
	List<CategoriaEnVitrina> categoriasDe(UUID empresaId);

	/** Catálogo público (prendas no archivadas) de una empresa ACTIVA. Vacío si no está activa. */
	List<PrendaEnVitrina> catalogoDe(UUID empresaId, UUID categoriaId);

	/**
	 * Sucursales ACTIVAS (puntos de retiro) de una empresa ACTIVA, para que el cliente elija dónde
	 * retirar (RF-18.5). Vacío si la empresa no existe o no está activa.
	 */
	List<SucursalEnVitrina> sucursalesActivasDe(UUID empresaId);
}
