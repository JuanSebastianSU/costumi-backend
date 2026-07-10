package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.ListarEmpleados;

import java.util.List;
import java.util.UUID;

/** DTO de salida de un empleado en el listado del tenant (G1): rol, estado y sucursales. Sin contraseña. */
record EmpleadoDetalleResponse(UUID id, String email, String rol, boolean activo, List<UUID> sucursales) {

	static EmpleadoDetalleResponse desde(ListarEmpleados.EmpleadoDelTenant e) {
		return new EmpleadoDetalleResponse(e.id(), e.email(), e.rol().name(), e.activo(), e.sucursales());
	}
}
