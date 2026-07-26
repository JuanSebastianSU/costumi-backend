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

	/** Horario de atención público de la tienda (A7), una franja por día que abre, ordenado por día. */
	List<HorarioEnVitrina> horarioDe(UUID empresaId);

	/**
	 * Disfraces destacados del marketplace (carrusel, C1): cruza tiendas ACTIVAS, ordenados por movimiento
	 * (venta+renta) y luego por nombre. {@code limite} = cuántos traer.
	 */
	List<DisfrazDestacado> destacados(int limite);

	/** Catálogo público (prendas no archivadas) de una empresa ACTIVA. Vacío si no está activa. */
	List<PrendaEnVitrina> catalogoDe(UUID empresaId, UUID categoriaId);

	/**
	 * Por cada prenda del catálogo público (mismos filtros que {@link #catalogoDe}), los ids de valor de
	 * etiqueta que tiene asignados. El service los resuelve a nombre. Indexado por id de prenda.
	 */
	java.util.Map<UUID, List<UUID>> valorEtiquetasDeCatalogo(UUID empresaId, UUID categoriaId);

	/**
	 * Sucursales ACTIVAS (puntos de retiro) de una empresa ACTIVA, para que el cliente elija dónde
	 * retirar (RF-18.5). Vacío si la empresa no existe o no está activa.
	 */
	List<SucursalEnVitrina> sucursalesActivasDe(UUID empresaId);
}
