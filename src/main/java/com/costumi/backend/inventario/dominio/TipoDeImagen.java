package com.costumi.backend.inventario.dominio;

import java.util.Optional;

/**
 * Tipos de imagen permitidos para la foto de una prenda (RF-2.9, C1). La detección es por <b>magic bytes</b>
 * del contenido real, no por el {@code content-type} ni el nombre del archivo (que el cliente puede falsear):
 * así se evita subir contenido que no es una imagen y se deriva una extensión/tipo canónicos y seguros.
 */
public enum TipoDeImagen {

	JPEG(".jpg", "image/jpeg"),
	PNG(".png", "image/png"),
	WEBP(".webp", "image/webp");

	private final String extension;
	private final String contentType;

	TipoDeImagen(String extension, String contentType) {
		this.extension = extension;
		this.contentType = contentType;
	}

	public String extension() {
		return extension;
	}

	public String contentType() {
		return contentType;
	}

	/** Detecta el tipo por la firma del contenido; vacío si no es un JPEG/PNG/WEBP válido. */
	public static Optional<TipoDeImagen> detectar(byte[] contenido) {
		if (contenido == null) {
			return Optional.empty();
		}
		if (empiezaCon(contenido, 0xFF, 0xD8, 0xFF)) {
			return Optional.of(JPEG);
		}
		if (empiezaCon(contenido, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
			return Optional.of(PNG);
		}
		if (esWebp(contenido)) {
			return Optional.of(WEBP);
		}
		return Optional.empty();
	}

	private static boolean empiezaCon(byte[] contenido, int... firma) {
		if (contenido.length < firma.length) {
			return false;
		}
		for (int i = 0; i < firma.length; i++) {
			if ((contenido[i] & 0xFF) != firma[i]) {
				return false;
			}
		}
		return true;
	}

	/** Contenedor WEBP: "RIFF"…"WEBP" (bytes 0-3 y 8-11). */
	private static boolean esWebp(byte[] c) {
		return c.length >= 12
				&& c[0] == 'R' && c[1] == 'I' && c[2] == 'F' && c[3] == 'F'
				&& c[8] == 'W' && c[9] == 'E' && c[10] == 'B' && c[11] == 'P';
	}
}
