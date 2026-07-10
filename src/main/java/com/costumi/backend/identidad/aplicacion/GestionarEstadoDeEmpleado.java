package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Usuario;

import java.util.UUID;

/** Puerto de entrada: dar de baja / reactivar a un empleado de la empresa (RF-8), acotado al tenant. */
public interface GestionarEstadoDeEmpleado {

	/**
	 * Da de baja al empleado. {@code actorRol}/{@code actorId} = quién lo hace: solo sobre empleados
	 * estrictamente por debajo suyo en la pirámide (RF-1.3, B3), nunca sobre sí mismo ni sobre un igual.
	 */
	Usuario desactivar(UUID empresaId, Rol actorRol, UUID actorId, UUID usuarioId);

	/** Reactiva a un empleado dado de baja (mismas restricciones de jerarquía). */
	Usuario activar(UUID empresaId, Rol actorRol, UUID usuarioId);
}
