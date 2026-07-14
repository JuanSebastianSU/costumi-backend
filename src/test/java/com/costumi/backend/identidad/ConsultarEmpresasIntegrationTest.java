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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Listado de Empresas ACTIVAS/SUSPENDIDAS del SuperAdmin (RF-15.3): la lista para suspender/reactivar. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ConsultarEmpresasIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private String superAdmin() throws Exception {
		return AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
	}

	private UUID registrarEmpresa(String nombre) throws Exception {
		String body = mvc.perform(post("/api/v1/empresas")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	@Test
	void lista_activas_y_suspendidas_con_su_estado_pero_no_pendientes_ni_rechazadas() throws Exception {
		String token = superAdmin();
		String marca = " " + UUID.randomUUID();

		UUID activa = registrarEmpresa("Activa" + marca);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", activa).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		UUID suspendida = registrarEmpresa("Suspendida" + marca);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", suspendida).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
		mvc.perform(post("/api/v1/empresas/{id}/suspender", suspendida).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		UUID pendiente = registrarEmpresa("Pendiente" + marca);
		UUID rechazada = registrarEmpresa("Rechazada" + marca);
		mvc.perform(post("/api/v1/empresas/{id}/rechazar", rechazada).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mvc.perform(get("/api/v1/empresas").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == '" + activa + "')].estado").value("ACTIVA"))
				.andExpect(jsonPath("$[?(@.id == '" + suspendida + "')].estado").value("SUSPENDIDA"))
				.andExpect(jsonPath("$[?(@.id == '" + pendiente + "')]").isEmpty())
				.andExpect(jsonPath("$[?(@.id == '" + rechazada + "')]").isEmpty());
	}

	@Test
	void con_rol_no_superadmin_devuelve_403() throws Exception {
		UUID empresa = registrarEmpresa("Empresa del dueño " + UUID.randomUUID());
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);

		mvc.perform(get("/api/v1/empresas").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isForbidden());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/empresas"))
				.andExpect(status().isUnauthorized());
	}
}
