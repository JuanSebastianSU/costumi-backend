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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rebanada 2: decisiones del SuperAdmin sobre la Empresa (RF-15.3), extremo a extremo. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class GestionarEmpresaIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	private UUID registrarEmpresa(String nombre) throws Exception {
		String body = mvc.perform(post("/api/v1/empresas")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		JsonNode node = json.readTree(body);
		return UUID.fromString(node.get("id").asText());
	}

	@Test
	void aprobar_una_pendiente_la_activa() throws Exception {
		UUID id = registrarEmpresa("Aprobar SA");

		mvc.perform(post("/api/v1/empresas/{id}/aprobar", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("ACTIVA"));
	}

	@Test
	void suspender_una_activa_la_suspende() throws Exception {
		UUID id = registrarEmpresa("Suspender SA");
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", id)).andExpect(status().isOk());

		mvc.perform(post("/api/v1/empresas/{id}/suspender", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("SUSPENDIDA"));
	}

	@Test
	void aprobar_una_ya_rechazada_devuelve_409() throws Exception {
		UUID id = registrarEmpresa("Rechazada SA");
		mvc.perform(post("/api/v1/empresas/{id}/rechazar", id)).andExpect(status().isOk());

		mvc.perform(post("/api/v1/empresas/{id}/aprobar", id))
				.andExpect(status().isConflict());
	}

	@Test
	void gestionar_una_empresa_inexistente_devuelve_404() throws Exception {
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", UUID.randomUUID()))
				.andExpect(status().isNotFound());
	}
}
