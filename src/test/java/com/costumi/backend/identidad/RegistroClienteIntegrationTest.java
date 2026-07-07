package com.costumi.backend.identidad;

import com.costumi.backend.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Auto-registro de CLIENTE (usuario final del marketplace): crea cuenta sin empresa y auto-login. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegistroClienteIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	private String email() {
		return "cliente-" + UUID.randomUUID() + "@correo.com";
	}

	@Test
	void un_cliente_se_registra_y_queda_logueado_sin_empresa() throws Exception {
		String correo = email();
		String body = mvc.perform(post("/api/v1/auth/registro")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"password\":\"clave1234\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists())
				.andReturn().getResponse().getContentAsString();

		String token = json.readTree(body).get("accessToken").asText();
		// El token es de un CLIENTE y NO tiene empresa (no pertenece a ningún tenant).
		mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rol").value("CLIENTE"))
				.andExpect(jsonPath("$.empresaId").doesNotExist());
	}

	@Test
	void el_cliente_registrado_puede_iniciar_sesion() throws Exception {
		String correo = email();
		mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"password\":\"clave1234\"}"))
				.andExpect(status().isOk());

		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"password\":\"clave1234\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists());
	}

	@Test
	void email_duplicado_devuelve_409() throws Exception {
		String correo = email();
		String req = "{\"email\":\"" + correo + "\",\"password\":\"clave1234\"}";
		mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(req))
				.andExpect(status().isOk());
		mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(req))
				.andExpect(status().isConflict());
	}

	@Test
	void password_corta_devuelve_400() throws Exception {
		mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email() + "\",\"password\":\"corta\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void el_registro_es_publico_y_normaliza_el_email_a_minusculas() throws Exception {
		String correo = email();
		mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo.toUpperCase() + "\",\"password\":\"clave1234\"}"))
				.andExpect(status().isOk());
		// Se guardó en minúsculas: el login con el correo en minúsculas funciona.
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"password\":\"clave1234\"}"))
				.andExpect(status().isOk());
	}
}
