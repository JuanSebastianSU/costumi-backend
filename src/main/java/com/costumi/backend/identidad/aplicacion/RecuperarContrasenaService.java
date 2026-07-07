package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.TokenDeRecuperacion;
import com.costumi.backend.identidad.dominio.TokenRecuperacionRepository;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Recuperación de contraseña (RF-1.1): genera un token de un solo uso con vencimiento, lo envía por
 * email (solo si el correo existe, sin revelarlo), y permite restablecer la contraseña con ese token.
 * Se guarda el <b>hash</b> del token (SHA-256), nunca el valor en claro.
 */
@Service
class RecuperarContrasenaService implements RecuperarContrasena {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final UsuarioRepository usuarios;
	private final TokenRecuperacionRepository tokens;
	private final PasswordEncoder passwordEncoder;
	private final EnviadorDeEmail email;
	private final Duration duracion;
	private final String urlBase;

	RecuperarContrasenaService(UsuarioRepository usuarios, TokenRecuperacionRepository tokens,
			PasswordEncoder passwordEncoder, EnviadorDeEmail email,
			@Value("${costumi.email.recuperacion.duracion-minutos:60}") long duracionMinutos,
			@Value("${costumi.email.recuperacion.url-base:}") String urlBase) {
		this.usuarios = usuarios;
		this.tokens = tokens;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.duracion = Duration.ofMinutes(duracionMinutos);
		this.urlBase = urlBase;
	}

	@Override
	@Transactional
	public void solicitar(String emailRaw) {
		String correo = emailRaw == null ? "" : emailRaw.trim().toLowerCase();
		usuarios.buscarPorEmail(correo).ifPresent(usuario -> {
			String token = tokenAleatorio();
			tokens.guardar(TokenDeRecuperacion.crear(usuario.id(), hash(token), Instant.now().plus(duracion)));
			email.enviar(usuario.email(), "Recuperación de contraseña — Costumi", cuerpo(token));
		});
		// Nunca se revela si el correo existe o no (evita enumeración de usuarios).
	}

	@Override
	@Transactional
	public void restablecer(String token, String nuevaPassword) {
		if (nuevaPassword == null || nuevaPassword.length() < 8) {
			throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
		}
		TokenDeRecuperacion registro = tokens.buscarPorHash(hash(token == null ? "" : token))
				.filter(t -> t.esVigente(Instant.now()))
				.orElseThrow(TokenDeRecuperacionInvalido::new);
		Usuario usuario = usuarios.buscarPorId(registro.usuarioId())
				.orElseThrow(TokenDeRecuperacionInvalido::new);
		usuarios.guardar(usuario.cambiarContrasena(passwordEncoder.encode(nuevaPassword)));
		registro.marcarUsado();
		tokens.guardar(registro);
	}

	private String cuerpo(String token) {
		String enlace = urlBase.isBlank() ? "" : "\n\nEnlace: " + urlBase + "?token=" + token;
		return "Recibimos una solicitud para restablecer tu contraseña.\n\n"
				+ "Tu código de recuperación es:\n" + token + enlace
				+ "\n\nSi no fuiste vos, ignorá este mensaje. El código vence en "
				+ duracion.toMinutes() + " minutos.";
	}

	private static String tokenAleatorio() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String hash(String token) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(md.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 no disponible", e);
		}
	}
}
