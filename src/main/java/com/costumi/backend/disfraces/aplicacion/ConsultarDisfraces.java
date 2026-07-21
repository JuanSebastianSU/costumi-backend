package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.Disfraz;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Puerto de entrada: listado de disfraces de la empresa (tenant). */
public interface ConsultarDisfraces {

	/** Todos los disfraces de la empresa (incluidos los archivados): vista de gestión. */
	List<Disfraz> deEmpresa(UUID empresaId);

	/** Solo los disfraces activos: vista pública (vitrina del marketplace). */
	List<Disfraz> activosDeEmpresa(UUID empresaId);

	/**
	 * Todos los valores SUGERIDOS de un disfraz en un solo cálculo: rango (mín–máx) de renta y venta y multa
	 * por tipo (daño/reposición), a partir de los precios/valores de sus elementos (RF-2.10). Con slots fijos
	 * mín == máx (precio directo); con personalizables el rango se abre según qué opción se elija. Son una
	 * pista para el dueño al armar el disfraz; él puede fijar {@code precioRentaGeneral}/{@code precioVentaGeneral}
	 * que la anulen. Carga el catálogo de la empresa UNA vez (sin consultar stock por pieza).
	 */
	Sugeridos sugeridosDe(UUID empresaId, Disfraz disfraz);

	/**
	 * Los {@link Sugeridos} de varios disfraces cargando el catálogo de la empresa <b>una sola vez</b> y
	 * resolviendo cada slot en memoria. Es la vía para listas (gestión y vitrina): evita el N+1 de recalcular
	 * precios prenda por prenda. Devuelve un mapa {@code disfrazId -> Sugeridos}.
	 */
	Map<UUID, Sugeridos> sugeridosDe(UUID empresaId, List<Disfraz> disfraces);

	/** Rango (mín–máx) de renta y venta sugeridos + multa sugerida por tipo del disfraz. */
	record Sugeridos(BigDecimal rentaMin, BigDecimal rentaMax, BigDecimal ventaMin, BigDecimal ventaMax,
			MultaSugerida multa) {
	}

	/** Rango de multa sugerido del disfraz por tipo (daño y reposición/pérdida). */
	record MultaSugerida(BigDecimal danoMin, BigDecimal danoMax, BigDecimal reposicionMin, BigDecimal reposicionMax) {
	}
}
