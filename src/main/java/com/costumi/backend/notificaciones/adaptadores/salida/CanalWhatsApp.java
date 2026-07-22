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
 * Canal WhatsApp vía Meta Cloud API (RF-11.4), <b>gateado</b>: si no hay token/phone-id configurados o
 * el cliente no tiene teléfono, no envía (devuelve false; el router cae al log). Con credenciales, envía.
 */
@Component
class CanalWhatsApp implements CanalDeNotificacion {

	private static final Logger log = LoggerFactory.getLogger(CanalWhatsApp.class);

	private final ContactoDelCliente contacto;
	private final String token;
	private final String phoneId;

	// TODO(credenciales): COSTUMI_WHATSAPP_TOKEN y COSTUMI_WHATSAPP_PHONE_ID (Meta Cloud API).
	CanalWhatsApp(ContactoDelCliente contacto,
			@Value("${costumi.notificaciones.whatsapp.token:}") String token,
			@Value("${costumi.notificaciones.whatsapp.phone-id:}") String phoneId) {
		this.contacto = contacto;
		this.token = token;
		this.phoneId = phoneId;
	}

	boolean configurado() {
		return !token.isBlank() && !phoneId.isBlank();
	}

	@Override
	public boolean enviar(Notificacion notificacion) {
		if (!configurado()) {
			return false;
		}
		String telefono = contacto.buscar(notificacion.empresaId(), notificacion.clienteId()).map(ContactoDeCliente::telefono).orElse(null);
		if (telefono == null || telefono.isBlank()) {
			return false;
		}
		try {
			RestClient.create().post()
					.uri("https://graph.facebook.com/v20.0/{phoneId}/messages", phoneId)
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("messaging_product", "whatsapp", "to", telefono, "type", "text",
							"text", Map.of("body", notificacion.mensaje())))
					.retrieve().toBodilessEntity();
			return true;
		} catch (Exception e) {
			log.error("Fallo enviando WhatsApp a cliente {}", notificacion.clienteId(), e);
			return false;
		}
	}
}
