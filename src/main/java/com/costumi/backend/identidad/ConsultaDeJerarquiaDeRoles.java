package com.costumi.backend.identidad;

/**
 * API pública de la pirámide de roles (B3) para otros módulos (§5.5): permite autorizar un <b>override</b>
 * (p. ej. revertir la decisión de un reembolso) exigiendo que el actor supere en jerarquía a quien decidió,
 * sin exponer el enum {@code Rol} interno de identidad.
 */
public interface ConsultaDeJerarquiaDeRoles {

	/** ¿El {@code rolActor} está estrictamente por encima del {@code rolObjetivo} en la pirámide? */
	boolean superaEstrictamente(String rolActor, String rolObjetivo);
}
