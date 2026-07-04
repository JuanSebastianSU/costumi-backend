package com.costumi.backend.identidad;

import com.costumi.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rebanada end-to-end del auto-registro (RF-15.2): HTTP -> aplicación -> Postgres real. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegistrarEmpresaIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Test
	void registrar_empresa_devuelve_201_y_nace_pendiente() throws Exception {
		mvc.perform(post("/api/v1/empresas")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Disfraces Pirata\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.nombre").value("Disfraces Pirata"))
				.andExpect(jsonPath("$.estado").value("PENDIENTE"));
	}

	@Test
	void nombre_vacio_devuelve_400() throws Exception {
		mvc.perform(post("/api/v1/empresas")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"\"}"))
				.andExpect(status().isBadRequest());
	}
}
