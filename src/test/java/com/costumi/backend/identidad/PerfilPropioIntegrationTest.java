package com.costumi.backend.identidad;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.identidad.dominio.Rol;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Perfil propio (RF-14): antes el usuario no tenía forma de editar sus datos ni de cambiar su contraseña
 * estando dentro — la única vía era el correo de recuperación.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PerfilPropioIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void un_cliente_ve_y_edita_su_perfil() throws Exception {
		String cliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);

		// Sin nombre cargado, el nombre para mostrar es el correo.
		String antes = mvc.perform(get("/api/v1/perfil").header("Authorization", "Bearer " + cliente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").doesNotExist())
				.andExpect(jsonPath("$.rol").value("CLIENTE"))
				.andReturn().getResponse().getContentAsString();
		String email = json.readTree(antes).get("email").asText();
		org.assertj.core.api.Assertions.assertThat(json.readTree(antes).get("nombreParaMostrar").asText())
				.isEqualTo(email);

		mvc.perform(put("/api/v1/perfil").header("Authorization", "Bearer " + cliente)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Juan Perez\",\"telefono\":\"0999123456\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Juan Perez"))
				.andExpect(jsonPath("$.telefono").value("0999123456"))
				.andExpect(jsonPath("$.nombreParaMostrar").value("Juan Perez"));

		// Persistió: se ve en una consulta nueva.
		mvc.perform(get("/api/v1/perfil").header("Authorization", "Bearer " + cliente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Juan Perez"));
	}

	@Test
	void cambiar_la_contrasena_exige_la_actual() throws Exception {
		String email = "perfil-" + UUID.randomUUID() + "@costumi.test";
		mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"Costumi123!\"}"))
				.andExpect(status().isOk());
		String token = login(email, "Costumi123!");

		// Con la actual equivocada no cambia nada.
		mvc.perform(post("/api/v1/perfil/contrasena").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"contrasenaActual\":\"NoEsLaMia1!\",\"contrasenaNueva\":\"NuevaClave1!\"}"))
				.andExpect(status().isUnauthorized());

		// Con la actual correcta, sí.
		mvc.perform(post("/api/v1/perfil/contrasena").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"contrasenaActual\":\"Costumi123!\",\"contrasenaNueva\":\"NuevaClave1!\"}"))
				.andExpect(status().isNoContent());

		// La vieja ya no sirve y la nueva sí.
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"Costumi123!\"}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"NuevaClave1!\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void una_contrasena_nueva_demasiado_corta_se_rechaza() throws Exception {
		String email = "perfil-corta-" + UUID.randomUUID() + "@costumi.test";
		mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"Costumi123!\"}"))
				.andExpect(status().isOk());
		String token = login(email, "Costumi123!");

		mvc.perform(post("/api/v1/perfil/contrasena").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"contrasenaActual\":\"Costumi123!\",\"contrasenaNueva\":\"corta\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void sin_token_no_se_puede_ver_ni_editar_el_perfil() throws Exception {
		mvc.perform(get("/api/v1/perfil")).andExpect(status().isUnauthorized());
		mvc.perform(put("/api/v1/perfil").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Nadie\"}"))
				.andExpect(status().isUnauthorized());
	}

	private String login(String email, String password) throws Exception {
		String res = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return json.readTree(res).get("accessToken").asText();
	}
}
