package com.costumi.backend.identidad;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Auth end-to-end (RF-17.4): login emite JWT y un recurso protegido lo valida. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private String sembrarSuperAdmin(String email, String password) {
		usuarios.guardar(Usuario.crear(null, email, passwordEncoder.encode(password), Rol.SUPERADMIN));
		return email;
	}

	@Test
	void login_valido_emite_token_y_me_devuelve_la_identidad() throws Exception {
		String email = "sa-" + UUID.randomUUID() + "@costumi.test";
		sembrarSuperAdmin(email, "secret123");

		String body = mvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andReturn().getResponse().getContentAsString();
		String token = json.readTree(body).get("accessToken").asText();

		mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.rol").value("SUPERADMIN"));
	}

	@Test
	void refresh_renueva_el_acceso_y_rechaza_un_token_de_acceso() throws Exception {
		String email = "sa-" + UUID.randomUUID() + "@costumi.test";
		sembrarSuperAdmin(email, "secret123");
		String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.refreshToken").exists())
				.andReturn().getResponse().getContentAsString();
		String refresh = json.readTree(body).get("refreshToken").asText();
		String access = json.readTree(body).get("accessToken").asText();

		// Con el token de refresco se obtiene un nuevo par (RF-1.1).
		mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + refresh + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists())
				.andExpect(jsonPath("$.refreshToken").exists());

		// Un token de acceso NO sirve para refrescar -> 401.
		mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + access + "\"}"))
				.andExpect(status().isUnauthorized());

		// Un token basura -> 401.
		mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"no-es-un-jwt\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void login_con_password_incorrecta_devuelve_401() throws Exception {
		String email = "sa-" + UUID.randomUUID() + "@costumi.test";
		sembrarSuperAdmin(email, "secret123");

		mvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"incorrecta\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void acceder_a_me_sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/auth/me"))
				.andExpect(status().isUnauthorized());
	}
}
