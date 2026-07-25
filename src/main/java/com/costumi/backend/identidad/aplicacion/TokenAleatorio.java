package com.costumi.backend.identidad.aplicacion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Genera tokens de un solo uso y su hash (mismo patrón que la recuperación de contraseña): el valor en claro
 * viaja por email/enlace, y se persiste solo el <b>hash SHA-256</b>, nunca el claro.
 */
final class TokenAleatorio {

	private static final SecureRandom RANDOM = new SecureRandom();

	private TokenAleatorio() {
	}

	/** Un token aleatorio de 32 bytes en Base64 URL sin padding. */
	static String generar() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** SHA-256 hex del token (lo que se guarda y con lo que se busca). */
	static String hash(String token) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(md.digest((token == null ? "" : token).getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 no disponible", e);
		}
	}
}
