package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.aplicacion.EmisorDeTokens;
import com.costumi.backend.identidad.aplicacion.RefreshEmitido;
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
	private final Duration expiracionRefresh;
	private final String issuer;

	EmisorDeTokensJwt(JwtEncoder encoder,
			@Value("${costumi.security.jwt.expiracion-minutos:60}") long expiracionMinutos,
			@Value("${costumi.security.jwt.refresh-dias:7}") long refreshDias,
			@Value("${costumi.security.jwt.issuer:costumi}") String issuer) {
		this.encoder = encoder;
		this.expiracion = Duration.ofMinutes(expiracionMinutos);
		this.expiracionRefresh = Duration.ofDays(refreshDias);
		this.issuer = issuer;
	}

	@Override
	public String emitir(Usuario usuario) {
		return emitir(usuario, expiracion, "access", null);
	}

	@Override
	public RefreshEmitido emitirRefresh(Usuario usuario, String jti) {
		Instant expiraEn = Instant.now().plus(expiracionRefresh);
		String token = emitir(usuario, expiracionRefresh, "refresh", jti);
		return new RefreshEmitido(token, expiraEn);
	}

	private String emitir(Usuario usuario, Duration duracion, String uso, String jti) {
		Instant ahora = Instant.now();
		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.issuedAt(ahora)
				.expiresAt(ahora.plus(duracion))
				.subject(usuario.id().toString())
				.claim("email", usuario.email())
				.claim("rol", usuario.rol().name())
				.claim("token_use", uso);
		if (jti != null) {
			claims.id(jti); // claim "jti": permite registrar y revocar el refresh server-side (C2)
		}
		if (usuario.empresaId() != null) {
			claims.claim("empresa_id", usuario.empresaId().toString());
		}
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
	}
}
