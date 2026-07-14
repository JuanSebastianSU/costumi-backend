package com.costumi.backend.notificaciones;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.identidad.AuthTestHelper;
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

/** Plantillas de mensajes automáticos (RF-11): listado con defaults + personalización por empresa. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PlantillaNotificacionIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private String duenoDeEmpresaNueva() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Plantillas " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String sa = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + sa))
				.andExpect(status().isOk());
		return AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
	}

	@Test
	void lista_las_seis_categorias_con_su_default_activo() throws Exception {
		String dueno = duenoDeEmpresaNueva();

		mvc.perform(get("/api/v1/notificaciones/plantillas").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(6))
				.andExpect(jsonPath("$[?(@.tipo == 'MULTA_GENERADA')].activa").value(true));
	}

	@Test
	void personalizar_una_plantilla_persiste_texto_y_switch() throws Exception {
		String dueno = duenoDeEmpresaNueva();

		mvc.perform(put("/api/v1/notificaciones/plantillas/MULTA_GENERADA").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"texto\":\"Hola {cliente}, te pasaste: {monto}\",\"activa\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.texto").value("Hola {cliente}, te pasaste: {monto}"))
				.andExpect(jsonPath("$.activa").value(false));

		// Al releer, la personalización se mantiene (y las demás siguen con su default).
		mvc.perform(get("/api/v1/notificaciones/plantillas").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.tipo == 'MULTA_GENERADA')].activa").value(false))
				.andExpect(jsonPath("$[?(@.tipo == 'MULTA_GENERADA')].texto").value("Hola {cliente}, te pasaste: {monto}"));
	}

	@Test
	void tipo_desconocido_devuelve_404() throws Exception {
		String dueno = duenoDeEmpresaNueva();

		mvc.perform(put("/api/v1/notificaciones/plantillas/NO_EXISTE").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"texto\":\"x\",\"activa\":true}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/notificaciones/plantillas")).andExpect(status().isUnauthorized());
	}
}
