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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gestión de empleados (RF-8): el alta es por invitación (ver {@link InvitacionIntegrationTest}); acá se
 * prueban listado, cambio de rol, baja/reactivación de cuenta y la pirámide (B3) sobre empleados ya activos.
 */
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

	/** Invita y acepta: deja un empleado ACTIVO y devuelve su usuarioId. */
	private UUID crearEmpleadoActivo(String token, String email, String rol) throws Exception {
		String res = mvc.perform(post("/api/v1/empleados").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"rol\":\"" + rol + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String enlace = json.readTree(res).get("enlace").asText(); // urlBase vacío ⇒ enlace = token
		mvc.perform(post("/api/v1/invitaciones/aceptar").contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"" + enlace + "\",\"password\":\"secret123\",\"aceptaTerminos\":true}"))
				.andExpect(status().isOk());
		return usuarios.buscarPorEmail(email).orElseThrow().id();
	}

	/** Solo invita (para chequear el status de autorización/validación del alta). */
	private void invitar(String token, String email, String rol, int esperado) throws Exception {
		mvc.perform(post("/api/v1/empleados").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"rol\":\"" + rol + "\"}"))
				.andExpect(status().is(esperado));
	}

	@Test
	void dar_de_baja_la_cuenta_impide_iniciar_sesion_y_reactivarla_lo_habilita() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		String email = "baja-" + UUID.randomUUID() + "@costumi.test";
		UUID empleadoId = crearEmpleadoActivo(dueno, email, "MOSTRADOR");

		// Login OK antes de la baja.
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
				.andExpect(status().isOk());

		// El dueño da de baja la cuenta.
		mvc.perform(post("/api/v1/empleados/{id}/desactivar", empleadoId).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activo").value(false));

		// Ya no puede iniciar sesión (RF-8) -> 403 aunque la contraseña sea correcta.
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
				.andExpect(status().isForbidden());

		// Reactivarla lo habilita de nuevo.
		mvc.perform(post("/api/v1/empleados/{id}/activar", empleadoId).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activo").value(true));
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void no_se_puede_dar_de_baja_a_un_empleado_de_otra_empresa_404() throws Exception {
		UUID empresaA = empresaAprobada();
		String duenoA = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresaA, Rol.DUENO);
		UUID empleadoDeA = crearEmpleadoActivo(duenoA, "ajeno-" + UUID.randomUUID() + "@costumi.test", "BODEGA");

		// El dueño de otra empresa no puede tocarlo (aislamiento por tenant) -> 404.
		UUID empresaB = empresaAprobada();
		String duenoB = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresaB, Rol.DUENO);
		mvc.perform(post("/api/v1/empleados/{id}/desactivar", empleadoDeA).header("Authorization", "Bearer " + duenoB))
				.andExpect(status().isNotFound());
	}

	@Test
	void no_se_puede_invitar_a_un_superadmin_400() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		invitar(dueno, "sa-" + UUID.randomUUID() + "@costumi.test", "SUPERADMIN", 400);
	}

	@Test
	void un_mostrador_no_puede_invitar_empleados_403() throws Exception {
		UUID empresa = empresaAprobada();
		String mostrador = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.MOSTRADOR);
		invitar(mostrador, "x-" + UUID.randomUUID() + "@costumi.test", "BODEGA", 403);
	}

	@Test
	void el_dueno_lista_su_personal_con_rol_y_estado() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		crearEmpleadoActivo(dueno, "mos-" + UUID.randomUUID() + "@costumi.test", "MOSTRADOR");
		crearEmpleadoActivo(dueno, "bod-" + UUID.randomUUID() + "@costumi.test", "BODEGA");

		// G1: el dueño ve a quienes puede gestionar (los 2 creados), no a sí mismo; con rol y estado.
		mvc.perform(get("/api/v1/empleados").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido.length()").value(2))
				.andExpect(jsonPath("$.contenido[?(@.rol == 'MOSTRADOR')]").exists())
				.andExpect(jsonPath("$.contenido[?(@.rol == 'BODEGA')]").exists())
				.andExpect(jsonPath("$.contenido[?(@.rol == 'DUENO')]").doesNotExist())
				.andExpect(jsonPath("$.contenido[0].activo").value(true))
				.andExpect(jsonPath("$.contenido[0].email").exists());
	}

	@Test
	void un_encargado_solo_ve_a_los_operativos_no_al_dueno_ni_a_los_encargados() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		String encargado = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.ENCARGADO);
		crearEmpleadoActivo(dueno, "mos-" + UUID.randomUUID() + "@costumi.test", "MOSTRADOR");

		mvc.perform(get("/api/v1/empleados").header("Authorization", "Bearer " + encargado))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.rol == 'MOSTRADOR')]").exists())
				.andExpect(jsonPath("$.contenido[?(@.rol == 'DUENO')]").doesNotExist())
				.andExpect(jsonPath("$.contenido[?(@.rol == 'ENCARGADO')]").doesNotExist());
	}

	@Test
	void un_mostrador_no_puede_listar_el_personal_403() throws Exception {
		UUID empresa = empresaAprobada();
		String mostrador = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.MOSTRADOR);
		mvc.perform(get("/api/v1/empleados").header("Authorization", "Bearer " + mostrador))
				.andExpect(status().isForbidden());
	}

	@Test
	void el_dueno_asciende_un_mostrador_a_encargado() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID mostrador = crearEmpleadoActivo(dueno, "m-" + UUID.randomUUID() + "@costumi.test", "MOSTRADOR");

		// G2: el dueño puede fijar un rol por debajo suyo (ENCARGADO).
		mvc.perform(put("/api/v1/empleados/{id}/rol", mostrador).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"ENCARGADO\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rol").value("ENCARGADO"));
	}

	@Test
	void nadie_puede_cambiar_a_un_empleado_a_dueno_403() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID mostrador = crearEmpleadoActivo(dueno, "m-" + UUID.randomUUID() + "@costumi.test", "MOSTRADOR");

		// B3: nadie fija el rol DUEÑO por esta vía (ni el propio dueño: no está por debajo suyo).
		mvc.perform(put("/api/v1/empleados/{id}/rol", mostrador).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"DUENO\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void un_encargado_no_puede_ascender_a_alguien_a_encargado_403() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		String encargado = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.ENCARGADO);
		UUID mostrador = crearEmpleadoActivo(dueno, "m-" + UUID.randomUUID() + "@costumi.test", "MOSTRADOR");

		// B3: el encargado no puede fijar un rol de su mismo nivel (ENCARGADO).
		mvc.perform(put("/api/v1/empleados/{id}/rol", mostrador).header("Authorization", "Bearer " + encargado)
						.contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"ENCARGADO\"}"))
				.andExpect(status().isForbidden());

		// Pero sí puede moverlo entre roles operativos (por debajo suyo).
		mvc.perform(put("/api/v1/empleados/{id}/rol", mostrador).header("Authorization", "Bearer " + encargado)
						.contentType(MediaType.APPLICATION_JSON).content("{\"rol\":\"BODEGA\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rol").value("BODEGA"));
	}
}
