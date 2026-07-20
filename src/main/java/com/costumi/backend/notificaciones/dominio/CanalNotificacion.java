package com.costumi.backend.notificaciones.dominio;

/** Canal por el que se envía una notificación (RF-11.4 WhatsApp, RF-18.11 FCM). Lista extensible. */
public enum CanalNotificacion {

	WHATSAPP,
	FCM,
	EMAIL,
	/** Aviso in-app para el equipo del negocio (dueño): se persiste y aparece en la app, sin canal externo. */
	IN_APP
}
