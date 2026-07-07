package com.costumi.backend.inventario.aplicacion;

/** Puerto de salida: almacenamiento de imágenes (fotos de prenda, RF-2.9). */
public interface AlmacenDeImagenes {

	/** ¿Está configurado (con credenciales/bucket) para poder subir? */
	boolean disponible();

	/**
	 * Sube el contenido y devuelve la URL pública. Lanza {@link AlmacenDeImagenesNoConfigurado} si no
	 * hay credenciales cargadas.
	 */
	String subir(byte[] contenido, String contentType, String clave);
}
