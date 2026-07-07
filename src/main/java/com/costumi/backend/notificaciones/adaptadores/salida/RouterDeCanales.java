package com.costumi.backend.notificaciones.adaptadores.salida;

import com.costumi.backend.notificaciones.dominio.CanalDeNotificacion;
import com.costumi.backend.notificaciones.dominio.Notificacion;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Canal de notificación principal: despacha según {@code notificacion.canal()} al adaptador real
 * (WhatsApp/FCM). Si el canal no está configurado o el cliente no tiene contacto, cae al registro en
 * log (gateado), para no romper el flujo. Es el bean {@link Primary} que usa el servicio de notificaciones.
 */
@Component
@Primary
class RouterDeCanales implements CanalDeNotificacion {

	private final CanalWhatsApp whatsApp;
	private final CanalFcm fcm;
	private final CanalDeNotificacionLog registroEnLog;

	RouterDeCanales(CanalWhatsApp whatsApp, CanalFcm fcm, CanalDeNotificacionLog registroEnLog) {
		this.whatsApp = whatsApp;
		this.fcm = fcm;
		this.registroEnLog = registroEnLog;
	}

	@Override
	public boolean enviar(Notificacion notificacion) {
		boolean enviado = switch (notificacion.canal()) {
			case WHATSAPP -> whatsApp.enviar(notificacion);
			case FCM -> fcm.enviar(notificacion);
			case EMAIL -> false; // notificación por email: no hay canal externo aún, va al log
		};
		return enviado || registroEnLog.enviar(notificacion);
	}
}
