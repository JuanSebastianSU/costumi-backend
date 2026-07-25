package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Capacidad;

import java.util.UUID;

/** Puerto de entrada: chequeo de capacidades para la autorización de requests (Fase B, paso 5). */
public interface ConsultaDePermisos {

	/**
	 * ¿El dueño desactivó explícitamente esta capacidad para el empleado? Solo bloquea con un override
	 * explícito {@code concedido=false}; sin override, la autorización la resuelve el rol (SecurityConfig).
	 */
	boolean bloqueado(UUID usuarioId, Capacidad capacidad);
}
