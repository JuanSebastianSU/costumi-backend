package com.costumi.backend.compartido;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Acceso centralizado al tenant/rol del usuario autenticado — base del aislamiento multi-tenant
 * forzado (§5.4). Lee el {@code empresa_id}, el rol y el id de usuario del token JWT del contexto
 * de seguridad de la petición, en un solo lugar (en vez de repartir la extracción por cada controller).
 *
 * <p>Es la API pública del módulo compartido; el resto de módulos lo usan para no reinventar la
 * extracción del tenant y para poder endurecerla luego (filtro Hibernate / RLS) sin tocarlos.
 */
@Component
public class ContextoDeTenant {

	/** Empresa del token, si la hay (el SuperAdmin de plataforma no tiene). */
	public Optional<UUID> empresaId() {
		return jwt().map(j -> j.getClaimAsString("empresa_id"))
				.filter(valor -> valor != null && !valor.isBlank())
				.map(UUID::fromString);
	}

	/** Empresa del token; si el usuario no pertenece a una empresa, deniega el acceso (403). */
	public UUID empresaIdRequerida() {
		return empresaId().orElseThrow(AccesoSinEmpresa::new);
	}

	/** Rol del token (RF-17.4). */
	public Optional<String> rol() {
		return jwt().map(j -> j.getClaimAsString("rol"));
	}

	/** Id del usuario autenticado (el empleado que realiza la operación, RF-1.4). */
	public Optional<UUID> usuarioId() {
		return jwt().map(Jwt::getSubject).map(UUID::fromString);
	}

	private static Optional<Jwt> jwt() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth instanceof JwtAuthenticationToken token) {
			return Optional.of(token.getToken());
		}
		return Optional.empty();
	}
}
