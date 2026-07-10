package com.costumi.backend.identidad;

import com.costumi.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rate limiting de autenticación (A2): tras N intentos por cuenta en la ventana, /auth/login responde 429. */
@SpringBootTest(properties = "costumi.security.rate-limit.auth.max=3")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RateLimitAuthIntegrationTest {

	@Autowired
	MockMvc mvc;

	private void login(String email, int esperado) throws Exception {
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"mala\"}"))
				.andExpect(status().is(esperado));
	}

	@Test
	void tras_superar_el_limite_de_login_devuelve_429() throws Exception {
		String email = "brute-" + UUID.randomUUID() + "@x.test";
		// Con max=3: los primeros 3 intentos (cuenta inexistente) dan 401; el 4º supera el límite -> 429.
		login(email, 401);
		login(email, 401);
		login(email, 401);
		login(email, 429);
	}

	@Test
	void el_limite_es_por_cuenta_otra_cuenta_no_se_bloquea() throws Exception {
		String agotada = "a-" + UUID.randomUUID() + "@x.test";
		login(agotada, 401);
		login(agotada, 401);
		login(agotada, 401);
		login(agotada, 429); // esta cuenta quedó limitada

		// Otra cuenta (otra clave) sigue pudiendo intentar: el límite no es global.
		String otra = "b-" + UUID.randomUUID() + "@x.test";
		login(otra, 401);
	}
}
