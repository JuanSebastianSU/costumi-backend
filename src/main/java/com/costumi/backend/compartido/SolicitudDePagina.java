package com.costumi.backend.compartido;

/**
 * Solicitud de una página de resultados (C3). Normaliza los valores del cliente: página &ge; 0 y tamaño
 * acotado entre 1 y {@link #TAMANO_MAXIMO} (con un default), para que ningún listado devuelva todo de golpe.
 */
public record SolicitudDePagina(int pagina, int tamano) {

	public static final int TAMANO_POR_DEFECTO = 20;
	public static final int TAMANO_MAXIMO = 100;

	public SolicitudDePagina {
		if (pagina < 0) {
			pagina = 0;
		}
		if (tamano < 1) {
			tamano = TAMANO_POR_DEFECTO;
		}
		if (tamano > TAMANO_MAXIMO) {
			tamano = TAMANO_MAXIMO;
		}
	}

	/** Construye desde parámetros opcionales del request (null = valores por defecto). */
	public static SolicitudDePagina de(Integer pagina, Integer tamano) {
		return new SolicitudDePagina(pagina == null ? 0 : pagina, tamano == null ? TAMANO_POR_DEFECTO : tamano);
	}
}
