package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.Prenda;

import java.util.UUID;

/** Puerto de entrada: subir/actualizar la foto de una prenda (RF-2.9). */
public interface AsignarFotoDePrenda {

	/**
	 * Sube el {@code contenido} como foto de la prenda. El tipo y la extensión se derivan del contenido real
	 * (magic bytes); no se confía en el content-type ni el nombre que envíe el cliente (C1).
	 */
	Prenda ejecutar(UUID empresaId, UUID prendaId, byte[] contenido);
}
