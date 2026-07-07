package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.aplicacion.EnviadorDeEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * Adaptador de email por SMTP, <b>gateado por configuración</b>: si no hay host SMTP configurado
 * (caso por defecto), registra el correo en el log y no falla. Cuando se cargan las credenciales
 * (ver docs/INFRA_PENDIENTE.md), envía de verdad. El dominio no sabe nada de esto.
 */
@Component
class EnviadorDeEmailSmtp implements EnviadorDeEmail {

	private static final Logger log = LoggerFactory.getLogger(EnviadorDeEmailSmtp.class);

	private final String host;
	private final int puerto;
	private final String usuario;
	private final String password;
	private final String remitente;

	// TODO(credenciales): COSTUMI_SMTP_HOST / COSTUMI_SMTP_PORT / COSTUMI_SMTP_USER / COSTUMI_SMTP_PASS
	//                     y COSTUMI_EMAIL_FROM. Sin host, el envío es no-op (solo log).
	EnviadorDeEmailSmtp(
			@Value("${costumi.email.smtp.host:}") String host,
			@Value("${costumi.email.smtp.port:587}") int puerto,
			@Value("${costumi.email.smtp.usuario:}") String usuario,
			@Value("${costumi.email.smtp.password:}") String password,
			@Value("${costumi.email.remitente:no-reply@costumi.co}") String remitente) {
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
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost(host);
		sender.setPort(puerto);
		sender.setUsername(usuario);
		sender.setPassword(password);
		sender.getJavaMailProperties().put("mail.smtp.auth", "true");
		sender.getJavaMailProperties().put("mail.smtp.starttls.enable", "true");

		SimpleMailMessage mensaje = new SimpleMailMessage();
		mensaje.setFrom(remitente);
		mensaje.setTo(destinatario);
		mensaje.setSubject(asunto);
		mensaje.setText(cuerpo);
		try {
			sender.send(mensaje);
		} catch (Exception e) {
			// No propagamos: el flujo de recuperación no debe revelar fallos internos al llamador.
			log.error("No se pudo enviar el email a {}", destinatario, e);
		}
	}
}
