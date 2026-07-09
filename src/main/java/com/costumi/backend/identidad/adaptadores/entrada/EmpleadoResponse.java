package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.dominio.Usuario;

import java.util.UUID;

/** DTO de salida de un empleado (nunca expone el hash de la contraseña). */
public record EmpleadoResponse(UUID id, String email, String rol, boolean activo) {

	static EmpleadoResponse desde(Usuario u) {
		return new EmpleadoResponse(u.id(), u.email(), u.rol().name(), u.activo());
	}
}
