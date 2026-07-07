package com.costumi.backend.notificaciones.adaptadores.salida;

import com.costumi.backend.notificaciones.dominio.CanalDeNotificacion;
import com.costumi.backend.notificaciones.dominio.ContactoDeCliente;
import com.costumi.backend.notificaciones.dominio.ContactoDelCliente;
import com.costumi.backend.notificaciones.dominio.Notificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Canal push FCM (RF-18.11), <b>gateado</b>: si no hay server key configurada o el cliente no tiene
 * device_token, no envía (false; el router cae al log). Con credencial, envía por HTTP a FCM.
 */
@Component
class CanalFcm implements CanalDeNotificacion {

	private static final Logger log = LoggerFactory.getLogger(CanalFcm.class);

	private final ContactoDelCliente contacto;
	private final String serverKey;

	// TODO(credenciales): COSTUMI_FCM_SERVER_KEY (HTTP legacy). Alternativa moderna: COSTUMI_FCM_CREDENTIALS
	//                     (service account, FCM HTTP v1) — requiere librería de auth de Google.
	CanalFcm(ContactoDelCliente contacto, @Value("${costumi.notificaciones.fcm.server-key:}") String serverKey) {
		this.contacto = contacto;
		this.serverKey = serverKey;
	}

	boolean configurado() {
		return !serverKey.isBlank();
	}

	@Override
	public boolean enviar(Notificacion notificacion) {
		if (!configurado()) {
			return false;
		}
		String deviceToken = contacto.buscar(notificacion.clienteId()).map(ContactoDeCliente::deviceToken).orElse(null);
		if (deviceToken == null || deviceToken.isBlank()) {
			return false;
		}
		try {
			RestClient.create().post()
					.uri("https://fcm.googleapis.com/fcm/send")
					.header("Authorization", "key=" + serverKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("to", deviceToken, "notification", Map.of("body", notificacion.mensaje())))
					.retrieve().toBodilessEntity();
			return true;
		} catch (Exception e) {
			log.error("Fallo enviando push FCM a cliente {}", notificacion.clienteId(), e);
			return false;
		}
	}
}
