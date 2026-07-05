package com.costumi.backend.compartido;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas del ContextoDeTenant: lee el token del SecurityContext, sin arrancar Spring. */
class ContextoDeTenantTest {

	private final ContextoDeTenant contexto = new ContextoDeTenant();

	@AfterEach
	void limpiar() {
		SecurityContextHolder.clearContext();
	}

	private static void autenticarCon(Jwt jwt) {
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
	}

	private static Jwt.Builder token() {
		return Jwt.withTokenValue("t").header("alg", "none")
				.issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
				.subject(UUID.randomUUID().toString());
	}

	@Test
	void extrae_empresa_y_rol_del_token() {
		UUID empresa = UUID.randomUUID();
		autenticarCon(token().claim("empresa_id", empresa.toString()).claim("rol", "DUENO").build());

		assertThat(contexto.empresaIdRequerida()).isEqualTo(empresa);
		assertThat(contexto.rol()).contains("DUENO");
		assertThat(contexto.usuarioId()).isPresent();
	}

	@Test
	void un_usuario_sin_empresa_no_puede_operar_sobre_un_tenant() {
		autenticarCon(token().claim("rol", "SUPERADMIN").build());

		assertThat(contexto.empresaId()).isEmpty();
		assertThatThrownBy(contexto::empresaIdRequerida).isInstanceOf(AccesoSinEmpresa.class);
	}

	@Test
	void sin_autenticacion_no_hay_empresa() {
		assertThat(contexto.empresaId()).isEmpty();
	}
}
