package com.costumi.backend.compartido;

import com.costumi.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El contrato OpenAPI (RF-17.3) se expone público y sin token en /v3/api-docs. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Test
	void el_contrato_openapi_es_publico() throws Exception {
		mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Costumi API"))
				.andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.scheme").value("bearer"));
	}

	@Test
	void el_contrato_publica_los_endpoints_del_cierre() throws Exception {
		// El contrato (fuente del cliente Kotlin) incluye los endpoints agregados en el cierre (RF-17.3).
		mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/v1/auth/refresh']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/empleados']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/pagos/mixto']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/reportes/rentas-vencidas']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/clientes/{id}/historial']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/notificaciones/recordar-vencidas']").exists());
	}

	@Test
	void los_endpoints_publicos_no_exigen_token_en_el_contrato() throws Exception {
		// Los públicos llevan security: [] (sobreescribe el requisito global) para no confundir al cliente.
		mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security").isEmpty())
				.andExpect(jsonPath("$.paths['/api/v1/auth/registro'].post.security").isEmpty())
				.andExpect(jsonPath("$.paths['/api/v1/empresas'].post.security").isEmpty())
				.andExpect(jsonPath("$.paths['/api/v1/pagos/webhook'].post.security").isEmpty())
				.andExpect(jsonPath("$.paths['/api/v1/marketplace/empresas'].get.security").isEmpty())
				// Un endpoint protegido conserva el requisito global (no se le pone security propia).
				.andExpect(jsonPath("$.paths['/api/v1/auth/me'].get.security").doesNotExist());
	}
}
