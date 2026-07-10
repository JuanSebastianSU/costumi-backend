package com.costumi.backend.inventario.aplicacion;

/**
 * El archivo subido como foto de prenda no es una imagen soportada (JPEG/PNG/WEBP), según sus magic bytes
 * (RF-2.9, C1). Se traduce a HTTP 415 (Unsupported Media Type).
 */
public class FormatoDeImagenNoSoportado extends RuntimeException {

	public FormatoDeImagenNoSoportado() {
		super("El archivo no es una imagen soportada (JPEG, PNG o WEBP)");
	}
}
