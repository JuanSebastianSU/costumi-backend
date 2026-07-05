package com.costumi.backend.notificaciones.aplicacion;

import com.costumi.backend.notificaciones.dominio.Notificacion;

/** Puerto de entrada: enviar una notificación (RF-11). */
public interface EnviarNotificacion {

	Notificacion ejecutar(EnviarNotificacionComando comando);
}
