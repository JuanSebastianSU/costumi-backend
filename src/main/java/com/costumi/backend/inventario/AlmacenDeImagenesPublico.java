package com.costumi.backend.inventario;

/**
 * API pública del módulo Inventario para que otros módulos (p. ej. Disfraces) reutilicen el MISMO
 * almacenamiento de imágenes (S3) sin conocer sus clases internas. Detecta el formato por magic bytes
 * (C1) y devuelve la URL pública. Vive en el paquete base del módulo (parte de lo que Inventario expone).
 */
public interface AlmacenDeImagenesPublico {

	/**
	 * Sube el {@code contenido} como imagen bajo la {@code claveBase} (a la que se le añade un id único y la
	 * extensión detectada); devuelve la URL pública. Lanza si el contenido no es una imagen soportada o si el
	 * almacén no está configurado (ambos casos ya los mapea el manejador de errores global).
	 */
	String subir(byte[] contenido, String claveBase);
}
