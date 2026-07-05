package com.costumi.backend.identidad;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.identidad.dominio.Rol;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cola de empresas PENDIENTES (RF-15.4), restringida a SuperAdmin. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ConsultarEmpresasPendientesIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void el_superadmin_ve_la_cola_con_una_recien_registrada_no_vencida() throws Exception {
		String token = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		String nombre = "Pendiente-" + UUID.randomUUID();
		mvc.perform(post("/api/v1/empresas")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated());

		String body = mvc.perform(get("/api/v1/empresas/pendientes").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		JsonNode lista = json.readTree(body);
		boolean encontrada = false;
		for (JsonNode nodo : lista) {
			if (nombre.equals(nodo.get("nombre").asText())) {
				encontrada = true;
				assertThat(nodo.get("vencida").asBoolean()).isFalse();
			}
		}
		assertThat(encontrada).isTrue();
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/empresas/pendientes"))
				.andExpect(status().isUnauthorized());
	}
}
