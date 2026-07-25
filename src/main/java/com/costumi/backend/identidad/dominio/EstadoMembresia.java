package com.costumi.backend.identidad.dominio;

/**
 * Estado de una membresía de trabajo (Fase B). Ciclo: nace {@code ACTIVA} al aceptar la invitación; el dueño
 * puede {@code SUSPENDIDA} (reversible). Las bajas son definitivas y se distinguen por quién las hizo, porque
 * cambia cómo se vuelve (decisión #5): {@code BAJA_DUENO} (despido/quita — reversible re-invitando) y
 * {@code BAJA_EMPLEADO} (el empleado se fue — para volver hace falta re-invitación + aceptación).
 */
public enum EstadoMembresia {
	ACTIVA,
	SUSPENDIDA,
	BAJA_EMPLEADO,
	BAJA_DUENO
}
