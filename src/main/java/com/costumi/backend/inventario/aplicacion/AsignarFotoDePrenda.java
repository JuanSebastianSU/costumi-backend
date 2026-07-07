package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.Prenda;

import java.util.UUID;

/** Puerto de entrada: subir/actualizar la foto de una prenda (RF-2.9). */
public interface AsignarFotoDePrenda {

	Prenda ejecutar(UUID empresaId, UUID prendaId, byte[] contenido, String contentType, String nombreArchivo);
}
