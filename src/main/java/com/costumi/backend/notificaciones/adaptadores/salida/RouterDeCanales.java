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
class RouterDeCanales implements CanalDeNotificacion,
		com.costumi.backend.notificaciones.aplicacion.ConsultarEstadoDeCanales {

	private final CanalWhatsApp whatsApp;
	private final CanalFcm fcm;
	private final CanalDeNotificacionLog registroEnLog;

	RouterDeCanales(CanalWhatsApp whatsApp, CanalFcm fcm, CanalDeNotificacionLog registroEnLog) {
		this.whatsApp = whatsApp;
		this.fcm = fcm;
		this.registroEnLog = registroEnLog;
	}

	/** Estado de los canales externos, para diagnosticar por que un aviso cayo al log. */
	@Override
	public com.costumi.backend.notificaciones.aplicacion.EstadoDeCanales estado() {
		return new com.costumi.backend.notificaciones.aplicacion.EstadoDeCanales(
				fcm.configurado(), fcm.diagnostico(), whatsApp.configurado());
	}

	@Override
	public com.costumi.backend.notificaciones.aplicacion.ResultadoDePrueba probarPush(java.util.UUID empresaId,
			java.util.UUID clienteId) {
		CanalFcm.ResultadoDeEnvio r = fcm.probar(empresaId, clienteId);
		return new com.costumi.backend.notificaciones.aplicacion.ResultadoDePrueba(r.enviado(), r.detalle());
	}

	@Override
	public boolean enviar(Notificacion notificacion) {
		boolean enviado = switch (notificacion.canal()) {
			case WHATSAPP -> whatsApp.enviar(notificacion);
			case FCM -> fcm.enviar(notificacion);
			case EMAIL -> false; // notificación por email: no hay canal externo aún, va al log
			case IN_APP -> false; // aviso in-app para el dueño: sin canal externo, solo se persiste (log)
		};
		return enviado || registroEnLog.enviar(notificacion);
	}
}
