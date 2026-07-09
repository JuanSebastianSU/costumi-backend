package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Usuario;

import java.util.UUID;

/** Puerto de entrada: dar de baja / reactivar a un empleado de la empresa (RF-8), acotado al tenant. */
public interface GestionarEstadoDeEmpleado {

	/** Da de baja al empleado. {@code actorId} = quién lo hace (no puede darse de baja a sí mismo). */
	Usuario desactivar(UUID empresaId, UUID actorId, UUID usuarioId);

	/** Reactiva a un empleado dado de baja. */
	Usuario activar(UUID empresaId, UUID usuarioId);
}
