package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.aplicacion.EmisorDeTokens;
import com.costumi.backend.identidad.dominio.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Adaptador de salida: emite el token como JWT (HS256), con las claims empresa+rol (§5.6).
 */
@Component
class EmisorDeTokensJwt implements EmisorDeTokens {

	private final JwtEncoder encoder;
	private final Duration expiracion;

	EmisorDeTokensJwt(JwtEncoder encoder,
			@Value("${costumi.security.jwt.expiracion-minutos:60}") long expiracionMinutos) {
		this.encoder = encoder;
		this.expiracion = Duration.ofMinutes(expiracionMinutos);
	}

	@Override
	public String emitir(Usuario usuario) {
		Instant ahora = Instant.now();
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.issuer("costumi")
				.issuedAt(ahora)
				.expiresAt(ahora.plus(expiracion))
				.subject(usuario.id().toString())
				.claim("email", usuario.email())
				.claim("rol", usuario.rol().name());
		if (usuario.empresaId() != null) {
			claims.claim("empresa_id", usuario.empresaId().toString());
		}
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
	}
}
