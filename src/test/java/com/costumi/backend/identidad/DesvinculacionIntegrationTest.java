package com.costumi.backend.identidad;

import com.costumi.backend.TestcontainersConfiguration;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Desvinculación de dos vías (Fase B, paso 3): el dueño quita/suspende/reactiva; el empleado se va. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DesvinculacionIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void el_dueno_quita_al_empleado_y_queda_solo_cliente() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		String correo = "quit-" + UUID.randomUUID() + "@costumi.test";
		UUID empleadoId = crearEmpleadoActivo(dueno, correo, "MOSTRADOR");

		mvc.perform(post("/api/v1/empleados/{id}/quitar", empleadoId).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("BAJA_DUENO"));

		// Al re-loguearse es solo-cliente: no tiene contexto de trabajo ni puede entrar a «Trabajar».
		String tok = loginToken(correo, "secret123");
		mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + tok))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rol").value("CLIENTE"))
				.andExpect(jsonPath("$.membresiaActiva").doesNotExist());
		mvc.perform(post("/api/v1/auth/contexto").header("Authorization", "Bearer " + tok)
						.contentType(MediaType.APPLICATION_JSON).content("{\"modo\":\"TRABAJO\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void el_empleado_se_desvincula_por_su_cuenta() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		String correo = "irse-" + UUID.randomUUID() + "@costumi.test";
		crearEmpleadoActivo(dueno, correo, "MOSTRADOR");
		String tok = loginToken(correo, "secret123");

		mvc.perform(post("/api/v1/auth/me/desvincularme").header("Authorization", "Bearer " + tok))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("BAJA_EMPLEADO"));

		// Re-login: solo-cliente.
		mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + loginToken(correo, "secret123")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rol").value("CLIENTE"));
	}

	@Test
	void el_dueno_suspende_y_reactiva_una_membresia() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		String correo = "susp-" + UUID.randomUUID() + "@costumi.test";
		UUID empleadoId = crearEmpleadoActivo(dueno, correo, "MOSTRADOR");

		// Suspender: queda solo-cliente (reversible).
		mvc.perform(post("/api/v1/empleados/{id}/suspender", empleadoId).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("SUSPENDIDA"));
		mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + loginToken(correo, "secret123")))
				.andExpect(jsonPath("$.rol").value("CLIENTE"));

		// Reactivar: vuelve a ser MOSTRADOR (contexto de trabajo restaurado).
		mvc.perform(post("/api/v1/empleados/{id}/reactivar", empleadoId).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("ACTIVA"));
		mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + loginToken(correo, "secret123")))
				.andExpect(jsonPath("$.rol").value("MOSTRADOR"));
	}

	// --- helpers ---

	private UUID crearEmpleadoActivo(String dueno, String correo, String rol) throws Exception {
		String res = mvc.perform(post("/api/v1/empleados").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"rol\":\"" + rol + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String enlace = json.readTree(res).get("enlace").asText();
		mvc.perform(post("/api/v1/invitaciones/aceptar").contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"" + enlace + "\",\"password\":\"secret123\",\"aceptaTerminos\":true}"))
				.andExpect(status().isOk());
		return usuarios.buscarPorEmail(correo).orElseThrow().id();
	}

	private String loginToken(String correo, String password) throws Exception {
		String res = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return json.readTree(res).get("accessToken").asText();
	}

	private UUID empresaAprobada() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Desv " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		return empresa;
	}
}
