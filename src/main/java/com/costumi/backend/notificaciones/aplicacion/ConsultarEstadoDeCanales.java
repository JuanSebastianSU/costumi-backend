package com.costumi.backend.notificaciones.aplicacion;

/** Puerto de entrada: estado de los canales externos (para diagnosticar envíos que caen al log). */
public interface ConsultarEstadoDeCanales {

	EstadoDeCanales estado();
}
