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

/** Ciclo de vida de la Empresa por el SuperAdmin (RF-15.3), ya con autorización por rol. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class GestionarEmpresaIntegrationTest {

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
		JsonNode node = json.readTree(body);
		return UUID.fromString(node.get("id").asText());
	}

	@Test
	void superadmin_aprueba_una_pendiente() throws Exception {
		String token = superAdmin();
		UUID id = registrarEmpresa("Aprobar SA");

		mvc.perform(post("/api/v1/empresas/{id}/aprobar", id).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("ACTIVA"));
	}

	@Test
	void superadmin_suspende_una_activa() throws Exception {
		String token = superAdmin();
		UUID id = registrarEmpresa("Suspender SA");
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", id).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mvc.perform(post("/api/v1/empresas/{id}/suspender", id).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("SUSPENDIDA"));
	}

	@Test
	void suspender_deja_traza_en_auditoria() throws Exception {
		String token = superAdmin();
		UUID id = registrarEmpresa("Auditada SA " + UUID.randomUUID());
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", id).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, id, Rol.DUENO);

		mvc.perform(post("/api/v1/empresas/{id}/suspender", id).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		// RF-15.5: la suspensión del SuperAdmin queda auditada (antes solo se auditaba la aprobación).
		mvc.perform(get("/api/v1/auditoria").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.accion == 'EMPRESA_SUSPENDIDA')]").exists());
	}

	@Test
	void aprobar_una_ya_rechazada_devuelve_409() throws Exception {
		String token = superAdmin();
		UUID id = registrarEmpresa("Rechazada SA");
		mvc.perform(post("/api/v1/empresas/{id}/rechazar", id).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mvc.perform(post("/api/v1/empresas/{id}/aprobar", id).header("Authorization", "Bearer " + token))
				.andExpect(status().isConflict());
	}

	@Test
	void gestionar_una_empresa_inexistente_devuelve_404() throws Exception {
		String token = superAdmin();

		mvc.perform(post("/api/v1/empresas/{id}/aprobar", UUID.randomUUID())
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		UUID id = registrarEmpresa("Sin token");

		mvc.perform(post("/api/v1/empresas/{id}/aprobar", id))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void con_rol_no_superadmin_devuelve_403() throws Exception {
		UUID empresaDelDueno = registrarEmpresa("Empresa del dueño");
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresaDelDueno, Rol.DUENO);
		UUID id = registrarEmpresa("Rol insuficiente");

		mvc.perform(post("/api/v1/empresas/{id}/aprobar", id).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isForbidden());
	}
}
