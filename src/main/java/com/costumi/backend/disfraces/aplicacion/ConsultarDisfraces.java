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

	/**
	 * Precio de VENTA sugerido del disfraz: la suma del precio de venta de sus prendas. Slot fijo → precio de
	 * venta de su prenda; slot personalizable → "desde" (el mínimo de las opciones). Es lo que se le muestra
	 * al dueño/cliente si el disfraz se vende (aparte de rentarse).
	 */
	BigDecimal precioVentaSugerido(UUID empresaId, Disfraz disfraz);

	/**
	 * Tope del rango de renta sugerido: la suma del precio de renta MÁS caro de cada slot. Con slots fijos
	 * coincide con {@link #precioRentaSugerido} (precio directo); con personalizables abre un rango
	 * mínimo–máximo según qué opción elija el cliente.
	 */
	BigDecimal precioRentaSugeridoMax(UUID empresaId, Disfraz disfraz);

	/** Tope del rango de venta sugerido (la opción más cara de cada slot); igual al mínimo si todo es fijo. */
	BigDecimal precioVentaSugeridoMax(UUID empresaId, Disfraz disfraz);
}
