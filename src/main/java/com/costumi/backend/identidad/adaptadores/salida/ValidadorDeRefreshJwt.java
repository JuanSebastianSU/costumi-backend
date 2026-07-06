package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.aplicacion.RefreshInvalido;
import com.costumi.backend.identidad.aplicacion.ValidadorDeRefresh;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida: valida el refresh como JWT (firma + vigencia con el {@link JwtDecoder} del
 * resource server) y exige que sea del tipo {@code refresh}, no de acceso (RF-1.1).
 */
@Component
class ValidadorDeRefreshJwt implements ValidadorDeRefresh {

	private final JwtDecoder decoder;

	ValidadorDeRefreshJwt(JwtDecoder decoder) {
		this.decoder = decoder;
	}

	@Override
	public String emailDelRefresh(String refreshToken) {
		try {
			Jwt jwt = decoder.decode(refreshToken);
			if (!"refresh".equals(jwt.getClaimAsString("token_use"))) {
				throw new RefreshInvalido(); // un token de acceso no sirve para refrescar
			}
			String email = jwt.getClaimAsString("email");
			if (email == null || email.isBlank()) {
				throw new RefreshInvalido();
			}
			return email;
		} catch (JwtException e) {
			throw new RefreshInvalido();
		}
	}
}
