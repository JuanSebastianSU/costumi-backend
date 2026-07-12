package com.costumi.backend.identidad;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Refresh revocable con rotación y detección de reuso (C2). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RefreshRevocableIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private String loginNuevo() throws Exception {
		String email = "u-" + UUID.randomUUID() + "@costumi.test";
		usuarios.guardar(Usuario.crear(null, email, passwordEncoder.encode("secret123"), Rol.CLIENTE));
		String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return json.readTree(body).get("refreshToken").asText();
	}

	private String refrescarOk(String refresh) throws Exception {
		String body = mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + refresh + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.refreshToken").exists())
				.andReturn().getResponse().getContentAsString();
		JsonNode node = json.readTree(body);
		return node.get("refreshToken").asText();
	}

	private void refrescarRechazado(String refresh) throws Exception {
		mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + refresh + "\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void la_rotacion_encadena_y_el_refresco_anterior_deja_de_servir() throws Exception {
		String r0 = loginNuevo();
		String r1 = refrescarOk(r0);   // r0 rota a r1
		String r2 = refrescarOk(r1);   // r1 rota a r2

		// El refresco intermedio ya rotado no sirve más (además dispara la revocación por reuso).
		refrescarRechazado(r1);
		// r2 pertenecía a la misma familia: la detección de reuso la anuló por completo.
		refrescarRechazado(r2);
	}

	@Test
	void reusar_el_refresco_inicial_revoca_toda_la_familia() throws Exception {
		String r0 = loginNuevo();
		String r1 = refrescarOk(r0);

		// Reuso del token ya consumido → 401 y revoca la familia.
		refrescarRechazado(r0);
		// El token vigente que había emitido la rotación también queda muerto.
		refrescarRechazado(r1);
	}

	@Test
	void logout_revoca_la_sesion() throws Exception {
		String r0 = loginNuevo();

		mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + r0 + "\"}"))
				.andExpect(status().isNoContent());

		refrescarRechazado(r0);
	}

	@Test
	void logout_es_idempotente_y_no_falla_con_token_basura() throws Exception {
		mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"no-es-un-jwt\"}"))
				.andExpect(status().isNoContent());

		String r0 = loginNuevo();
		mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + r0 + "\"}"))
				.andExpect(status().isNoContent());
		// Cerrar dos veces la misma sesión sigue devolviendo 204.
		mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + r0 + "\"}"))
				.andExpect(status().isNoContent());
	}
}
