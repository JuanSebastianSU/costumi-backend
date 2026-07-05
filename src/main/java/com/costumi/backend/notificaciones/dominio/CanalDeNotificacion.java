package com.costumi.backend.notificaciones.dominio;

/**
 * Puerto de salida: envía la notificación por su canal. Lo implementa un adaptador (WhatsApp/FCM/log).
 * Poca lógica, mucho adaptador (§5.2). Devuelve true si el envío fue aceptado.
 */
public interface CanalDeNotificacion {

	boolean enviar(Notificacion notificacion);
}
