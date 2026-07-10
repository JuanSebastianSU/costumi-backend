package com.costumi.backend.compartido;

import java.util.List;

/** Una página de resultados del dominio (C3): el contenido, el total de elementos y la posición. Neutral. */
public record Pagina<T>(List<T> contenido, long total, int pagina, int tamano) {

	public int totalPaginas() {
		return tamano <= 0 ? 0 : (int) Math.ceil((double) total / tamano);
	}

	public static <T> Pagina<T> de(List<T> contenido, long total, SolicitudDePagina solicitud) {
		return new Pagina<>(contenido, total, solicitud.pagina(), solicitud.tamano());
	}
}
