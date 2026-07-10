package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.AccionDePermiso;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Seccion;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: editor de permisos granular por empleado (RF-1.5). */
public interface GestionarPermisosDeEmpleado {

	/** Estado efectivo de una casilla: la sección/acción y si está concedida (plantilla ± override). */
	record PermisoEfectivo(Seccion seccion, AccionDePermiso accion, boolean concedido) {
	}

	/**
	 * Matriz efectiva completa del empleado (todas las secciones × acciones) partiendo de su rol. Solo si el
	 * actor ({@code actorRol}) tiene autoridad sobre el empleado (pirámide, RF-1.3/B3).
	 */
	List<PermisoEfectivo> matriz(UUID empresaId, Rol actorRol, UUID usuarioId);

	/**
	 * Activa/desactiva una casilla puntual encima de la plantilla del rol. Solo el actor con autoridad sobre
	 * el empleado (estrictamente por encima en la pirámide) puede hacerlo: así un empleado no se re-concede lo
	 * que un superior le quitó, ni edita a un igual/superior (RF-1.3/B3).
	 */
	void establecer(UUID empresaId, Rol actorRol, UUID usuarioId, Seccion seccion, AccionDePermiso accion,
			boolean concedido);
}
