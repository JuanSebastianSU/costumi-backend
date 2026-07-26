package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.aplicacion.EnviadorDeEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Adaptador de email por SMTP, <b>gateado por configuración</b>: si no hay host SMTP configurado
 * (caso por defecto), registra el correo en el log y no falla. Cuando se cargan las credenciales
 * (ver docs/INFRA_PENDIENTE.md), envía de verdad. El dominio no sabe nada de esto.
 *
 * <p>El envío real corre en <b>segundo plano</b>: {@link #enviar} vuelve al instante y no bloquea la
 * petición HTTP. Antes se enviaba en el hilo de la petición y, dentro de la transacción de "invitar
 * empleado", un SMTP lento/inaccesible colgaba el request varios minutos (la app daba timeout y parecía
 * que la invitación había fallado, aunque sí quedaba guardada). Además se acota el tiempo de conexión
 * para que un SMTP mal configurado falle rápido en vez de quedarse colgado.
 *
 * <p><b>Ya NO es el bean principal de {@link EnviadorDeEmail}</b>: en Railway el SMTP saliente está
 * bloqueado (el envío se cuelga y muere a los 10s), así que el bean primario es {@link EnviadorDeEmailHttp}
 * (API HTTP de Brevo, puerto 443). Esta clase queda como <b>respaldo</b> que ese adaptador usa cuando no
 * hay API HTTP configurada (útil en local/dev, donde el SMTP sí sale). Por eso ya no lleva {@code @Component}.
 */
class EnviadorDeEmailSmtp implements EnviadorDeEmail {

	private static final Logger log = LoggerFactory.getLogger(EnviadorDeEmailSmtp.class);

	/** Tope de espera para conectar/leer/escribir contra el SMTP (ms): que falle rápido, no que cuelgue. */
	private static final int TIMEOUT_MS = 10_000;

	private final String host;
	private final int puerto;
	private final String usuario;
	private final String password;
	private final String remitente;

	// Envío en segundo plano: hilos daemon para no impedir el apagado de la app.
	private final ExecutorService envios = Executors.newFixedThreadPool(2, r -> {
		Thread t = new Thread(r, "envio-email");
		t.setDaemon(true);
		return t;
	});

	// Lo construye EnviadorDeEmailHttp con los valores de config (COSTUMI_SMTP_*). Sin host, el envío es
	// no-op (solo log).
	EnviadorDeEmailSmtp(String host, int puerto, String usuario, String password, String remitente) {
		this.host = host;
		this.puerto = puerto;
		this.usuario = usuario;
		this.password = password;
		this.remitente = remitente;
	}

	@Override
	public void enviar(String destinatario, String asunto, String cuerpo) {
		if (host == null || host.isBlank()) {
			// Sin SMTP configurado: no se envía nada real, se registra (útil en dev y para no romper el flujo).
			log.info("[email:log] (sin SMTP) a {} — {}\n{}", destinatario, asunto, cuerpo);
			return;
		}
		// El envío real NO bloquea a quien invita: corre aparte y sus fallos solo se loguean.
		envios.submit(() -> enviarAhora(destinatario, asunto, cuerpo));
	}

	private void enviarAhora(String destinatario, String asunto, String cuerpo) {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost(host);
		sender.setPort(puerto);
		sender.setUsername(usuario);
		sender.setPassword(password);
		Properties props = sender.getJavaMailProperties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		// Que un SMTP inaccesible falle en ~10s en vez de colgarse.
		props.put("mail.smtp.connectiontimeout", String.valueOf(TIMEOUT_MS));
		props.put("mail.smtp.timeout", String.valueOf(TIMEOUT_MS));
		props.put("mail.smtp.writetimeout", String.valueOf(TIMEOUT_MS));

		SimpleMailMessage mensaje = new SimpleMailMessage();
		mensaje.setFrom(remitente);
		mensaje.setTo(destinatario);
		mensaje.setSubject(asunto);
		mensaje.setText(cuerpo);
		try {
			sender.send(mensaje);
		} catch (Exception e) {
			// No propagamos: el flujo no debe romperse porque el correo no salga (queda el enlace para compartir).
			log.error("No se pudo enviar el email a {}", destinatario, e);
		}
	}

	/** Lo llama {@link EnviadorDeEmailHttp} al apagarse (esta clase ya no es un bean con ciclo propio). */
	void cerrar() {
		envios.shutdown();
	}
}
