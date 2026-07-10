package com.costumi.backend.identidad;

import java.util.UUID;

/**
 * API pública de Identidad para que otros módulos comprueben si un empleado <b>puede operar en una
 * sucursal</b> (RF-1.2, B2), sin conocer las clases internas de Identidad.
 *
 * <p>Regla: los roles <b>acotados</b> (Mostrador/Bodega/Atención) solo pueden operar en las sucursales que
 * tienen <b>asignadas</b>; los roles de gestión (Dueño/Encargado) y los actores que no son empleados del
 * tenant (p. ej. el CLIENTE del marketplace) no están acotados por sucursal.
 */
public interface ConsultaDeAsignaciones {

	/**
	 * ¿El {@code empleadoId} puede operar en {@code sucursalId} dentro de {@code empresaId}? Devuelve
	 * {@code true} para roles no acotados o actores externos al tenant; para roles acotados, solo si la
	 * sucursal está entre sus asignadas.
	 */
	boolean empleadoPuedeOperarEn(UUID empresaId, UUID empleadoId, UUID sucursalId);
}
