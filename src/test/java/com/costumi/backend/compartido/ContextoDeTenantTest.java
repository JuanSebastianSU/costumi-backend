package com.costumi.backend.compartido;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
		RequestContextHolder.resetRequestAttributes();
	}

	private static void conCabeceraSucursal(String valor) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		if (valor != null) {
			request.addHeader("X-Sucursal-Id", valor);
		}
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
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

	@Test
	void lee_la_sucursal_activa_de_la_cabecera() {
		UUID sucursal = UUID.randomUUID();
		conCabeceraSucursal(sucursal.toString());

		assertThat(contexto.sucursalActiva()).contains(sucursal);
		assertThat(contexto.sucursalActivaRequerida()).isEqualTo(sucursal);
	}

	@Test
	void sin_cabecera_de_sucursal_falla_la_requerida() {
		conCabeceraSucursal(null);

		assertThat(contexto.sucursalActiva()).isEmpty();
		assertThatThrownBy(contexto::sucursalActivaRequerida).isInstanceOf(SucursalNoIndicada.class);
	}
}
