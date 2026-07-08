package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.AccionDePermiso;
import com.costumi.backend.identidad.dominio.Seccion;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: editor de permisos granular por empleado (RF-1.5). */
public interface GestionarPermisosDeEmpleado {

	/** Estado efectivo de una casilla: la sección/acción y si está concedida (plantilla ± override). */
	record PermisoEfectivo(Seccion seccion, AccionDePermiso accion, boolean concedido) {
	}

	/** Matriz efectiva completa del empleado (todas las secciones × acciones) partiendo de su rol. */
	List<PermisoEfectivo> matriz(UUID empresaId, UUID usuarioId);

	/** Activa/desactiva una casilla puntual encima de la plantilla del rol. */
	void establecer(UUID empresaId, UUID usuarioId, Seccion seccion, AccionDePermiso accion, boolean concedido);
}
