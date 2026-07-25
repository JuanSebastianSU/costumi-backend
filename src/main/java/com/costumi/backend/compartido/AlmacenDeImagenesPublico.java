package com.costumi.backend.compartido;

/**
 * Puerto compartido de almacenamiento de imágenes (S3): que cualquier módulo (Inventario, Disfraces,
 * Identidad para el logo/portada de la tienda y la foto de perfil…) reutilice el MISMO almacén sin conocer
 * sus clases internas. Vive en el kernel compartido para no acoplar unos módulos con otros (evita ciclos):
 * la implementación la aporta un módulo, pero todos dependen solo de esta interfaz.
 *
 * <p>Detecta el formato por magic bytes (C1) y devuelve la URL pública.
 */
public interface AlmacenDeImagenesPublico {

	/**
	 * Sube el {@code contenido} como imagen bajo la {@code claveBase} (a la que se le añade un id único y la
	 * extensión detectada); devuelve la URL pública. Lanza si el contenido no es una imagen soportada o si el
	 * almacén no está configurado (ambos casos ya los mapea el manejador de errores global).
	 */
	String subir(byte[] contenido, String claveBase);
}
