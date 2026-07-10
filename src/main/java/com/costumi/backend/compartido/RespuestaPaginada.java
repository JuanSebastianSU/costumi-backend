package com.costumi.backend.compartido;

import java.util.List;
import java.util.function.Function;

/** DTO de salida de una página (C3): forma estable para el cliente ({@code content} + metadatos). */
public record RespuestaPaginada<T>(List<T> contenido, long total, int pagina, int tamano, int totalPaginas) {

	/** Convierte una {@link Pagina} de dominio mapeando cada elemento a su DTO. */
	public static <D, T> RespuestaPaginada<T> desde(Pagina<D> pagina, Function<D, T> mapear) {
		return new RespuestaPaginada<>(
				pagina.contenido().stream().map(mapear).toList(),
				pagina.total(), pagina.pagina(), pagina.tamano(), pagina.totalPaginas());
	}
}
