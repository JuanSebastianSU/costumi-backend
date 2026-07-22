package com.costumi.backend.notificaciones.aplicacion;

/**
 * Estado de los canales externos de notificación. Sirve para responder "¿por qué no llegó la push?" sin
 * tener que mirar los logs del servidor ni destapar credenciales.
 *
 * <p>Hace falta porque el router, si un canal no está configurado, **cae al registro en log** y la
 * notificación queda igualmente como ENVIADA: desde afuera no se distingue un aviso que salió de uno que
 * solo se guardó.
 */
public record EstadoDeCanales(boolean fcmConfigurado, String fcmDetalle, boolean whatsAppConfigurado) {
}
