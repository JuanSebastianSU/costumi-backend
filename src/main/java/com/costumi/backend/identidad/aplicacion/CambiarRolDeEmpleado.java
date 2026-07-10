package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Usuario;

import java.util.UUID;

/** Puerto de entrada: cambiar el rol de un empleado ya creado (RF-8, G2). */
public interface CambiarRolDeEmpleado {

	/**
	 * Cambia el rol del empleado. El actor ({@code actorRol}) solo puede si tiene autoridad sobre el empleado
	 * (rol actual estrictamente por debajo suyo) <b>y</b> el nuevo rol también está estrictamente por debajo
	 * suyo (RF-1.3/B3): así nadie asciende a un igual/superior ni crea un DUEÑO por esta vía.
	 */
	Usuario ejecutar(UUID empresaId, Rol actorRol, UUID usuarioId, Rol nuevoRol);
}
