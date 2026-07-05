package com.costumi.backend.rentas.aplicacion;

import com.costumi.backend.rentas.dominio.Renta;

import java.util.UUID;

/** Puerto de entrada: transiciones de estado de una Renta (RF-3.5/3.6). */
public interface GestionarRenta {

	Renta entregar(UUID empresaId, UUID rentaId);

	Renta devolver(UUID empresaId, UUID rentaId);

	Renta cerrar(UUID empresaId, UUID rentaId);

	Renta cancelar(UUID empresaId, UUID rentaId);
}
