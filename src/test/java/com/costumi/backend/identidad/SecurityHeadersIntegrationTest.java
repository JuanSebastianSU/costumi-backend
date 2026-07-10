package com.costumi.backend.identidad;

import com.costumi.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Headers de seguridad en las respuestas (C4). Se prueban en un endpoint público del marketplace. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityHeadersIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Test
	void las_respuestas_llevan_headers_de_seguridad() throws Exception {
		mvc.perform(get("/api/v1/marketplace/empresas"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().string("Referrer-Policy", "no-referrer"))
				.andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
				.andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")));
	}

	@Test
	void sobre_https_se_emite_hsts() throws Exception {
		// request.isSecure() = true (proxy HTTPS): Spring emite Strict-Transport-Security.
		mvc.perform(get("/api/v1/marketplace/empresas").secure(true))
				.andExpect(status().isOk())
				.andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")))
				.andExpect(header().string("Strict-Transport-Security", containsString("includeSubDomains")));
	}
}
