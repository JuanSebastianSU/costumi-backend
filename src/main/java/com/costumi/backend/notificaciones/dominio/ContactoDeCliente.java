package com.costumi.backend.notificaciones.dominio;

/** Datos de contacto de un cliente para notificarlo: teléfono (WhatsApp) y token de dispositivo (FCM). */
public record ContactoDeCliente(String telefono, String deviceToken) {
}
