package com.costumi.backend.notificaciones.dominio;

/** Canal por el que se envía una notificación (RF-11.4 WhatsApp, RF-18.11 FCM). Lista extensible. */
public enum CanalNotificacion {

	WHATSAPP,
	FCM,
	EMAIL
}
