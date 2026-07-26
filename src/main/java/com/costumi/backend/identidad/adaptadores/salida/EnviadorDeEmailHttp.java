package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.aplicacion.EnviadorDeEmail;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enviador de email por <b>API HTTP</b> (Brevo), con SMTP como respaldo. Es el bean primario de
 * {@link EnviadorDeEmail}.
 *
 * <p><b>Por qué HTTP y no SMTP.</b> Railway <b>bloquea la salida SMTP</b> (puertos 25/465/587): el envío
 * por {@code smtp.gmail.com:587} se cuelga y muere por <i>connection timeout</i> a los 10s, así que el
 * correo (invitación de empleado, recuperación de contraseña) nunca llega, <b>aunque las credenciales de
 * Gmail sean correctas</b> (verificado: el mismo usuario/clave envía bien desde fuera de Railway). La API
 * HTTP de Brevo usa el puerto <b>443 (HTTPS)</b>, que Railway no bloquea.
 *
 * <p><b>Gateado por credencial.</b> Si {@code COSTUMI_BREVO_API_KEY} está seteada, envía por Brevo; si no,
 * delega en el {@link EnviadorDeEmailSmtp} de siempre (útil en local/dev, donde el SMTP sí sale, y a su vez
 * cae al log si tampoco hay SMTP). El dominio no sabe nada de esto (mismo puerto {@link EnviadorDeEmail}).
 *
 * <p>El remitente ({@code COSTUMI_EMAIL_FROM}) debe estar <b>verificado como sender en Brevo</b>.
 */
@Component
class EnviadorDeEmailHttp implements EnviadorDeEmail {

	private static final Logger log = LoggerFactory.getLogger(EnviadorDeEmailHttp.class);
	private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";
	/** Tope de conexión/lectura contra la API HTTP: que falle rápido, no que cuelgue. */
	private static final int TIMEOUT_MS = 10_000;

	private final String apiKey;
	private final String remitenteEmail;
	private final String remitenteNombre;
	private final EnviadorDeEmailSmtp fallback;
	private final RestClient http;

	// Envío en segundo plano: no bloquea la petición HTTP de quien invita/pide recuperación.
	private final ExecutorService envios = Executors.newFixedThreadPool(2, r -> {
		Thread t = new Thread(r, "envio-email-http");
		t.setDaemon(true);
		return t;
	});

	// TODO(credenciales): COSTUMI_BREVO_API_KEY (API HTTP). Sin ella, cae al SMTP (COSTUMI_SMTP_*).
	EnviadorDeEmailHttp(
			@Value("${costumi.email.brevo.api-key:}") String apiKey,
			@Value("${costumi.email.remitente:no-reply@costumi.co}") String remitenteEmail,
			@Value("${costumi.email.remitente-nombre:Costumi}") String remitenteNombre,
			// Respaldo SMTP: se arma con los mismos valores de config que usaba el adaptador SMTP.
			@Value("${costumi.email.smtp.host:}") String smtpHost,
			@Value("${costumi.email.smtp.port:587}") int smtpPuerto,
			@Value("${costumi.email.smtp.usuario:}") String smtpUsuario,
			@Value("${costumi.email.smtp.password:}") String smtpPassword) {
		this.apiKey = apiKey == null ? "" : apiKey.trim();
		this.remitenteEmail = remitenteEmail;
		this.remitenteNombre = remitenteNombre;
		this.fallback = new EnviadorDeEmailSmtp(smtpHost, smtpPuerto, smtpUsuario, smtpPassword, remitenteEmail);
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(TIMEOUT_MS);
		factory.setReadTimeout(TIMEOUT_MS);
		this.http = RestClient.builder().requestFactory(factory).build();
	}

	@Override
	public void enviar(String destinatario, String asunto, String cuerpo) {
		if (apiKey.isBlank()) {
			// Sin API HTTP configurada: usa el SMTP de siempre (que a su vez cae a log si no hay host).
			fallback.enviar(destinatario, asunto, cuerpo);
			return;
		}
		// El envío real NO bloquea a quien invita: corre aparte y sus fallos solo se loguean.
		envios.submit(() -> enviarAhora(destinatario, asunto, cuerpo));
	}

	private void enviarAhora(String destinatario, String asunto, String cuerpo) {
		try {
			http.post()
					.uri(BREVO_URL)
					.header("api-key", apiKey)
					.header("accept", "application/json")
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of(
							"sender", Map.of("email", remitenteEmail, "name", remitenteNombre),
							"to", List.of(Map.of("email", destinatario)),
							"subject", asunto,
							"textContent", cuerpo))
					.retrieve()
					.toBodilessEntity();
			log.info("Email enviado por HTTP (Brevo) a {}", destinatario);
		} catch (Exception e) {
			// No propagamos: el flujo no debe romperse porque el correo no salga (queda el enlace para compartir).
			log.error("No se pudo enviar el email por HTTP a {}", destinatario, e);
		}
	}

	@PreDestroy
	void cerrar() {
		envios.shutdown();
		fallback.cerrar();
	}
}
