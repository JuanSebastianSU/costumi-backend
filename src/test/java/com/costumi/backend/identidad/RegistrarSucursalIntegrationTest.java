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

/** Alta de Sucursal (RF-15.1) end-to-end, incluyendo la regla RF-15.4 (empresa ACTIVA). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegistrarSucursalIntegrationTest {

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

	private UUID registrarEmpresaActiva(String nombre) throws Exception {
		UUID id = registrarEmpresa(nombre);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", id)).andExpect(status().isOk());
		return id;
	}

	@Test
	void alta_de_sucursal_en_empresa_activa_devuelve_201() throws Exception {
		UUID empresaId = registrarEmpresaActiva("Empresa Activa");

		mvc.perform(post("/api/v1/empresas/{empresaId}/sucursales", empresaId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Centro\",\"direccion\":\"Calle 1\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.empresaId").value(empresaId.toString()))
				.andExpect(jsonPath("$.nombre").value("Centro"));
	}

	@Test
	void alta_de_sucursal_en_empresa_pendiente_devuelve_409() throws Exception {
		UUID empresaId = registrarEmpresa("Empresa Pendiente");

		mvc.perform(post("/api/v1/empresas/{empresaId}/sucursales", empresaId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Centro\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void alta_de_sucursal_en_empresa_inexistente_devuelve_404() throws Exception {
		mvc.perform(post("/api/v1/empresas/{empresaId}/sucursales", UUID.randomUUID())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Centro\"}"))
				.andExpect(status().isNotFound());
	}
}
