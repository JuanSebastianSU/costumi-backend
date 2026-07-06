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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Alta de empleados (RF-8): el dueño/encargado crea usuarios de su empresa; el empleado puede loguearse. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EmpleadoIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private UUID empresaAprobada() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Emp " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		return empresa;
	}

	private void crearEmpleado(String token, String email, String rol, int esperado) throws Exception {
		mvc.perform(post("/api/v1/empleados").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"secret123\",\"rol\":\"" + rol + "\"}"))
				.andExpect(status().is(esperado));
	}

	@Test
	void el_dueno_da_de_alta_un_empleado_que_puede_iniciar_sesion() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		String email = "empleado-" + UUID.randomUUID() + "@costumi.test";

		mvc.perform(post("/api/v1/empleados").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"secret123\",\"rol\":\"MOSTRADOR\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.rol").value("MOSTRADOR"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		// El empleado recién creado puede iniciar sesión.
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists());
	}

	@Test
	void correo_duplicado_devuelve_409() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		String email = "dup-" + UUID.randomUUID() + "@costumi.test";
		crearEmpleado(dueno, email, "BODEGA", 201);
		crearEmpleado(dueno, email, "BODEGA", 409);
	}

	@Test
	void no_se_puede_crear_un_superadmin_como_empleado_400() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		crearEmpleado(dueno, "sa-" + UUID.randomUUID() + "@costumi.test", "SUPERADMIN", 400);
	}

	@Test
	void un_mostrador_no_puede_dar_de_alta_empleados_403() throws Exception {
		UUID empresa = empresaAprobada();
		String mostrador = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.MOSTRADOR);
		crearEmpleado(mostrador, "x-" + UUID.randomUUID() + "@costumi.test", "BODEGA", 403);
	}
}
