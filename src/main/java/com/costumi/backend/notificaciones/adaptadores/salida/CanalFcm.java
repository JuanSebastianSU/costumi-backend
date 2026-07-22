package com.costumi.backend.notificaciones.adaptadores.salida;

import com.costumi.backend.notificaciones.dominio.CanalDeNotificacion;
import com.costumi.backend.notificaciones.dominio.ContactoDeCliente;
import com.costumi.backend.notificaciones.dominio.ContactoDelCliente;
import com.costumi.backend.notificaciones.dominio.Notificacion;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Canal push FCM (RF-18.11), <b>gateado</b>: si no hay credencial configurada o el cliente no tiene
 * {@code device_token}, no envía (devuelve false y el router cae al log).
 *
 * <p>Usa la <b>API HTTP v1</b>, firmando con la cuenta de servicio del proyecto Firebase. La API legacy
 * ({@code /fcm/send} con "server key") que usaba este canal <b>ya no existe</b>: Google la apagó, así que
 * el código anterior enviaba contra un endpoint muerto y nunca podía funcionar.
 *
 * <p>Configuración: {@code COSTUMI_FCM_CREDENTIALS} con el contenido del JSON de la cuenta de servicio.
 * El {@code project_id} se lee del propio JSON, así que no hay una segunda variable que se pueda
 * desincronizar.
 */
@Component
class CanalFcm implements CanalDeNotificacion {

	private static final Logger log = LoggerFactory.getLogger(CanalFcm.class);
	private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

	private final ContactoDelCliente contacto;
	private final String credencialJson;
	private final String projectId;

	CanalFcm(ContactoDelCliente contacto,
			@Value("${costumi.notificaciones.fcm.credentials:}") String credencialJson) {
		this.contacto = contacto;
		this.credencialJson = credencialJson == null ? "" : credencialJson.trim();
		this.projectId = leerProjectId(this.credencialJson);
	}

	boolean configurado() {
		return !credencialJson.isBlank() && projectId != null;
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
					.uri("https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send")
					.header("Authorization", "Bearer " + tokenDeAcceso())
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("message", Map.of(
							"token", deviceToken,
							"notification", Map.of("title", "Costumi", "body", notificacion.mensaje()))))
					.retrieve().toBodilessEntity();
			return true;
		} catch (Exception e) {
			log.error("Fallo enviando push FCM a cliente {}", notificacion.clienteId(), e);
			return false;
		}
	}

	/** Token OAuth de corta duración; la librería de Google lo cachea y lo renueva sola. */
	private String tokenDeAcceso() throws java.io.IOException {
		GoogleCredentials credenciales = GoogleCredentials
				.fromStream(new ByteArrayInputStream(credencialJson.getBytes(StandardCharsets.UTF_8)))
				.createScoped(List.of(SCOPE));
		credenciales.refreshIfExpired();
		return credenciales.getAccessToken().getTokenValue();
	}

	/** El proyecto viene dentro de la credencial: una variable menos que se pueda configurar mal. */
	private static String leerProjectId(String json) {
		if (json.isBlank()) {
			return null;
		}
		try {
			var nodo = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("project_id");
			return nodo == null || nodo.asText().isBlank() ? null : nodo.asText();
		} catch (Exception e) {
			// Credencial mal pegada: se apaga el push, pero la app arranca igual.
			log.error("La credencial de FCM no es un JSON valido; el push queda apagado", e);
			return null;
		}
	}
}
