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

/** Verifica que el SuperAdmin sembrado al inicio puede iniciar sesión. */
@SpringBootTest(properties = {
		"costumi.security.bootstrap.superadmin.email=boot@costumi.test",
		"costumi.security.bootstrap.superadmin.password=arranque123"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BootstrapSuperAdminIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Test
	void el_superadmin_de_bootstrap_puede_iniciar_sesion() throws Exception {
		mvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"boot@costumi.test\",\"password\":\"arranque123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists());
	}
}
