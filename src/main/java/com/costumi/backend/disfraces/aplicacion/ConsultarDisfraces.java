package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.Disfraz;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Puerto de entrada: listado de disfraces de la empresa (tenant). */
public interface ConsultarDisfraces {

	/** Todos los disfraces de la empresa (incluidos los archivados): vista de gestión. */
	List<Disfraz> deEmpresa(UUID empresaId);

	/** Solo los disfraces activos: vista pública (vitrina del marketplace). */
	List<Disfraz> activosDeEmpresa(UUID empresaId);

	/**
	 * Precio de renta SUGERIDO (por día) del disfraz: la suma del precio de renta de sus prendas (RF-2.10).
	 * Para slots personalizables (pool) toma el precio "desde" (el mínimo de las opciones). Es lo que se le
	 * muestra al dueño al armarlo; él puede fijar un {@code precioRentaGeneral} que lo anule.
	 */
	BigDecimal precioRentaSugerido(UUID empresaId, Disfraz disfraz);
}
