package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.AccionDePermiso;
import com.costumi.backend.identidad.dominio.Seccion;

import java.util.UUID;

/** Puerto de entrada: chequeo de permisos granulares para la autorización de requests (RF-1.5). */
public interface ConsultaDePermisos {

	/**
	 * ¿El dueño desactivó explícitamente esta casilla para el empleado? Solo bloquea con un override
	 * explícito {@code concedido=false}; sin override, la autorización la resuelve el rol (SecurityConfig).
	 */
	boolean bloqueado(UUID usuarioId, Seccion seccion, AccionDePermiso accion);
}
