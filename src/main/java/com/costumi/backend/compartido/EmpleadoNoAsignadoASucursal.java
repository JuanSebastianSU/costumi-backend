package com.costumi.backend.compartido;

import java.util.UUID;

/**
 * Un empleado de rol acotado (Mostrador/Bodega/Atención) intentó operar en una sucursal a la que <b>no está
 * asignado</b> (RF-1.2, B2). Se traduce a HTTP 403: para operar en otra sucursal, hay que asignárselo primero.
 */
public class EmpleadoNoAsignadoASucursal extends RuntimeException {

	public EmpleadoNoAsignadoASucursal(UUID sucursalId) {
		super("El empleado no está asignado a la sucursal " + sucursalId);
	}
}
