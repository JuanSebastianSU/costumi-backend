package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Usuario;

import java.util.UUID;

/** Puerto de entrada: alta de un empleado (usuario) para una empresa (RF-8). */
public interface AltaDeEmpleado {

	Usuario ejecutar(AltaDeEmpleadoComando comando);

	/**
	 * Datos del nuevo empleado. La contraseña llega en claro y se cifra en el caso de uso. {@code actorRol}
	 * es el rol de quien da el alta: solo puede crear roles estrictamente por debajo suyo (RF-1.3, B3).
	 */
	record AltaDeEmpleadoComando(UUID empresaId, Rol actorRol, String email, String password, Rol rol) {
	}
}
