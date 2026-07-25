package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Membresia;
import com.costumi.backend.identidad.dominio.Rol;

import java.util.UUID;

/**
 * Desvinculación de dos vías (Fase B, paso 3, decisión #5). El dueño (o quien tenga autoridad piramidal)
 * suspende/reactiva/da de baja a un empleado; el propio empleado puede irse. En todos los casos de baja la
 * persona <b>queda como solo-cliente</b> (conserva su cuenta y su faceta de compra); la diferencia entre baja
 * del dueño y del empleado es cómo se vuelve (re-invitación).
 */
public interface GestionarMembresiaDeEmpleado {

	/** El dueño suspende la membresía (corta el trabajo, reversible). El empleado queda solo-cliente. */
	Membresia suspender(UUID empresaId, Rol actorRol, UUID usuarioId);

	/** El dueño reactiva una membresía suspendida (vuelve a poder entrar a trabajar). */
	Membresia reactivar(UUID empresaId, Rol actorRol, UUID usuarioId);

	/** El dueño da de baja al empleado (despido). Definitivo; para volver hay que re-invitarlo. */
	Membresia quitar(UUID empresaId, Rol actorRol, UUID usuarioId);

	/** El propio empleado se desvincula de su tienda. Queda solo-cliente; para volver, re-invitación. */
	Membresia desvincularme(UUID usuarioId);
}
