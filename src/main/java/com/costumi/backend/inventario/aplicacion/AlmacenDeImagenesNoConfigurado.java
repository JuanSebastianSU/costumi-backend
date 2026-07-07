package com.costumi.backend.inventario.aplicacion;

/** El almacenamiento de imágenes no tiene credenciales cargadas (RF-2.9). Se traduce a 503. */
public class AlmacenDeImagenesNoConfigurado extends RuntimeException {

	public AlmacenDeImagenesNoConfigurado() {
		super("El almacenamiento de imágenes no está configurado. Cargá las credenciales de S3.");
	}
}
