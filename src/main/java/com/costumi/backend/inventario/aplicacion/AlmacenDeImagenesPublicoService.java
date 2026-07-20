package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.AlmacenDeImagenesPublico;
import com.costumi.backend.inventario.dominio.TipoDeImagen;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implementación de {@link AlmacenDeImagenesPublico}: envuelve el almacén interno (S3) y la detección de
 * formato para que cualquier módulo suba imágenes reutilizando la misma infraestructura y validación (C1).
 */
@Service
class AlmacenDeImagenesPublicoService implements AlmacenDeImagenesPublico {

	private final AlmacenDeImagenes almacen;

	AlmacenDeImagenesPublicoService(AlmacenDeImagenes almacen) {
		this.almacen = almacen;
	}

	@Override
	public String subir(byte[] contenido, String claveBase) {
		TipoDeImagen tipo = TipoDeImagen.detectar(contenido).orElseThrow(FormatoDeImagenNoSoportado::new);
		String clave = claveBase + UUID.randomUUID() + tipo.extension();
		return almacen.subir(contenido, tipo.contentType(), clave);
	}
}
