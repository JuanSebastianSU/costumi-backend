package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Sucursal;

import java.util.UUID;

/** Puerto de entrada: subir y asignar la foto de una sucursal (A7/G17), reusa el almacén compartido. */
public interface AsignarFotoDeSucursal {

	Sucursal asignarFoto(UUID empresaId, UUID sucursalId, byte[] contenido);
}
