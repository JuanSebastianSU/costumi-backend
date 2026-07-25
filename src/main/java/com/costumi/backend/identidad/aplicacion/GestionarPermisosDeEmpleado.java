package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Capacidad;
import com.costumi.backend.identidad.dominio.Rol;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: editor de la matriz de capacidades por empleado (Fase B, paso 5). */
public interface GestionarPermisosDeEmpleado {

	/** Estado efectivo de una capacidad para un empleado: la capacidad y si está concedida (preset ± override). */
	record CapacidadEfectiva(Capacidad capacidad, boolean concedido) {
	}

	/**
	 * Matriz efectiva completa del empleado (todas las capacidades del catálogo) partiendo del preset de su
	 * rol. Solo si el actor ({@code actorRol}) tiene autoridad sobre el empleado (pirámide, RF-1.3/B3).
	 */
	List<CapacidadEfectiva> matriz(UUID empresaId, Rol actorRol, UUID usuarioId);

	/**
	 * Las capacidades <b>concedidas</b> del PROPIO usuario (preset de su rol ± sus overrides), para que el
	 * front arme la navegación por permisos y no por rol. Sin chequeo de autoridad: uno siempre ve las suyas.
	 */
	List<Capacidad> mias(UUID usuarioId, Rol rol);

	/**
	 * Concede/niega una capacidad puntual encima del preset del rol. Reglas: el actor debe tener autoridad
	 * sobre el empleado (pirámide, RF-1.3/B3); y al <b>conceder</b>, el actor no puede dar una capacidad que él
	 * mismo no tiene ("no podés conceder lo que no tenés").
	 */
	void establecer(UUID empresaId, Rol actorRol, UUID actorId, UUID usuarioId, Capacidad capacidad, boolean concedido);
}
