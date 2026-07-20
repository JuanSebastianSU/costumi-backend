package com.costumi.backend.identidad;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.identidad.aplicacion.EscalarSolicitudesVencidas;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Escalación de solicitudes de empresa vencidas (RF-15.4). Con el plazo de resolución en 0, cualquier
 * solicitud PENDIENTE cuenta como vencida apenas se registra, así el job la detecta y la escala.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "costumi.empresa.plazo-resolucion-dias=0")
class EscalacionDeSolicitudesIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	EscalarSolicitudesVencidas escalarSolicitudesVencidas;

	@Test
	void escala_las_solicitudes_pendientes_que_pasaron_el_plazo() throws Exception {
		mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Escala " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated());

		// El job encuentra al menos la solicitud recién registrada como vencida y la escala.
		int escaladas = escalarSolicitudesVencidas.ejecutar();
		assertThat(escaladas).isGreaterThanOrEqualTo(1);
	}
}
