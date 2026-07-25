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

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Invitación/aceptación de empleados (Fase B, paso 3): alta = invitar, T&C, cuenta al aceptar, una-activa. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class InvitacionIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void invitar_y_aceptar_crea_un_empleado_nuevo_que_puede_trabajar() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		String correo = "nuevo-" + UUID.randomUUID() + "@costumi.test";

		String token = invitar(dueno, correo, "MOSTRADOR");

		// La persona ve la invitación desde el enlace: la tienda, el rol, y que necesita crear cuenta.
		mvc.perform(get("/api/v1/invitaciones/{token}", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rol").value("MOSTRADOR"))
				.andExpect(jsonPath("$.email").value(correo))
				.andExpect(jsonPath("$.necesitaCuenta").value(true));

		// Acepta (con T&C y contraseña) → queda logueada.
		aceptar(token, "secret123", true).andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty());

		// Puede iniciar sesión con la contraseña que puso.
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"password\":\"secret123\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void un_cliente_existente_acepta_y_se_le_suma_la_membresia() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		String correo = "cli-" + UUID.randomUUID() + "@costumi.test";
		// La persona ya es cliente del marketplace.
		mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"password\":\"cliente123\"}"))
				.andExpect(status().isOk());

		String token = invitar(dueno, correo, "ENCARGADO");
		// La vista dice que NO necesita crear cuenta (ya la tiene).
		mvc.perform(get("/api/v1/invitaciones/{token}", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.necesitaCuenta").value(false));

		// Acepta sin contraseña (ya tiene cuenta) → queda logueada y ve su membresía.
		String res = aceptar(token, null, true).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String tok = json.readTree(res).get("accessToken").asText();
		mvc.perform(get("/api/v1/auth/me/membresias").header("Authorization", "Bearer " + tok))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.empresaId == '" + empresa + "')].rol", hasItem("ENCARGADO")));
	}

	@Test
	void aceptar_sin_terminos_devuelve_400() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		String token = invitar(dueno, "nt-" + UUID.randomUUID() + "@costumi.test", "BODEGA");
		aceptar(token, "secret123", false).andExpect(status().isBadRequest());
	}

	@Test
	void token_invalido_devuelve_400() throws Exception {
		aceptar("token-que-no-existe", "secret123", true).andExpect(status().isBadRequest());
	}

	@Test
	void no_se_puede_invitar_a_quien_ya_trabaja_400() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		String correo = "ya-" + UUID.randomUUID() + "@costumi.test";
		aceptar(invitar(dueno, correo, "MOSTRADOR"), "secret123", true).andExpect(status().isOk());

		// Ya tiene una membresía activa → no se lo puede invitar de nuevo (regla de una-activa).
		mvc.perform(post("/api/v1/empleados").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"rol\":\"BODEGA\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void cancelar_una_invitacion_impide_aceptarla() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		String correo = "canc-" + UUID.randomUUID() + "@costumi.test";
		String token = invitar(dueno, correo, "MOSTRADOR");

		String pend = mvc.perform(get("/api/v1/empleados/invitaciones").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.email == '" + correo + "')]").exists())
				.andReturn().getResponse().getContentAsString();
		UUID invitacionId = UUID.fromString(json.readTree(pend).get(0).get("id").asText());

		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.delete("/api/v1/empleados/invitaciones/{id}", invitacionId)
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isNoContent());

		aceptar(token, "secret123", true).andExpect(status().isBadRequest());
	}

	@Test
	void la_sucursal_asignada_al_invitar_queda_al_aceptar() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"Centro\"}");
		String correo = "suc-" + UUID.randomUUID() + "@costumi.test";

		String res = mvc.perform(post("/api/v1/empleados").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"rol\":\"MOSTRADOR\",\"sucursalIds\":[\"" + sucursal + "\"]}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		aceptar(json.readTree(res).get("enlace").asText(), "secret123", true).andExpect(status().isOk());

		UUID empleadoId = usuarios.buscarPorEmail(correo).orElseThrow().id();
		mvc.perform(get("/api/v1/empleados/{id}/sucursales", empleadoId).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0]").value(sucursal.toString()));
	}

	// --- helpers ---

	/** Invita y devuelve el token de aceptación (enlace = token, con urlBase vacío en test). */
	private String invitar(String dueno, String correo, String rol) throws Exception {
		String res = mvc.perform(post("/api/v1/empleados").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"rol\":\"" + rol + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return json.readTree(res).get("enlace").asText();
	}

	private org.springframework.test.web.servlet.ResultActions aceptar(String token, String password,
			boolean terminos) throws Exception {
		String pass = password == null ? "" : "\"password\":\"" + password + "\",";
		return mvc.perform(post("/api/v1/invitaciones/aceptar").contentType(MediaType.APPLICATION_JSON)
				.content("{\"token\":\"" + token + "\"," + pass + "\"aceptaTerminos\":" + terminos + "}"));
	}

	private UUID empresaAprobada() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Inv " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		return empresa;
	}

	private UUID postId(String path, String token, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}
}
