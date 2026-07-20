package com.costumi.backend.disfraces.aplicacion;

import com.costumi.backend.disfraces.dominio.Disfraz;

import java.util.UUID;

/** Puerto de entrada: subir/actualizar la foto del disfraz que sube el dueño (RF-2.9/18.3). */
public interface AsignarFotoDeDisfraz {

	/**
	 * Sube el {@code contenido} como foto del disfraz (reutiliza el almacén de imágenes). El tipo/extensión
	 * se derivan del contenido real (magic bytes, C1); no se confía en lo que envíe el cliente.
	 */
	Disfraz ejecutar(UUID empresaId, UUID disfrazId, byte[] contenido);
}
