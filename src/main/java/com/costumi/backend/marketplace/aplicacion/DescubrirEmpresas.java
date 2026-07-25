package com.costumi.backend.marketplace.aplicacion;

import com.costumi.backend.compartido.Pagina;
import com.costumi.backend.compartido.SolicitudDePagina;
import com.costumi.backend.marketplace.dominio.CategoriaEnVitrina;
import com.costumi.backend.marketplace.dominio.EmpresaEnVitrina;
import com.costumi.backend.marketplace.dominio.PrendaEnVitrina;
import com.costumi.backend.marketplace.dominio.SucursalEnVitrina;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de entrada: descubrir las empresas ACTIVAS del marketplace (RF-18.1, RF-15.6). */
public interface DescubrirEmpresas {

	/** Página de empresas ACTIVAS; {@code buscar} (opcional) filtra por nombre (RF-18.1). */
	Pagina<EmpresaEnVitrina> empresas(String buscar, SolicitudDePagina solicitud);

	/** Detalle público de una tienda ACTIVA. Vacío si no existe o no está activa. */
	Optional<EmpresaEnVitrina> empresa(UUID empresaId);

	/** Facetas: categorías del catálogo público de la tienda. */
	List<CategoriaEnVitrina> categoriasDe(UUID empresaId);

	/** Catálogo público de una tienda (empresa ACTIVA). Vacío si no existe o no está activa. */
	/** Catálogo público de una tienda; si {@code categoriaId} no es null, filtra por esa categoría (RF-18.1). */
	List<PrendaEnVitrina> catalogo(UUID empresaId, UUID categoriaId);

	/**
	 * Sucursales (puntos de retiro) de una tienda ACTIVA, para que el cliente elija dónde retirar
	 * su renta/compra (RF-18.5). Vacío si la tienda no existe o no está activa.
	 */
	List<SucursalEnVitrina> sucursales(UUID empresaId);
}
