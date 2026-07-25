package com.costumi.backend.identidad;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.identidad.dominio.Membresia;
import com.costumi.backend.identidad.dominio.MembresiaRepository;
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

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Membresía de trabajo (H1, Fase B paso 2): una persona es siempre cliente y, si tiene una membresía activa,
 * alterna entre «Comprando» (token de cliente) y «Trabajando» (token con la empresa+rol de su membresía).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MembresiaIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	MembresiaRepository membresias;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void el_empleado_alterna_entre_comprar_y_trabajar() throws Exception {
		UUID empresa = crearEmpresaAprobada("A");
		AuthTestHelper.Sesion m = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa, Rol.MOSTRADOR);
		membresias.guardar(Membresia.crear(m.usuarioId(), empresa, Rol.MOSTRADOR));

		// Trabajando → token con la empresa+rol de la membresía.
		JsonNode trabajo = claimsDe(postContexto(m.token(), "TRABAJO"));
		assertThat(trabajo.get("empresa_id").asText()).isEqualTo(empresa.toString());
		assertThat(trabajo.get("rol").asText()).isEqualTo("MOSTRADOR");

		// Comprando → token de cliente (sin empresa, rol CLIENTE): la misma persona ahora puede comprar (H1).
		JsonNode compra = claimsDe(postContexto(m.token(), "COMPRA"));
		assertThat(compra.get("rol").asText()).isEqualTo("CLIENTE");
		assertThat(compra.has("empresa_id")).isFalse();
	}

	@Test
	void me_expone_la_membresia_activa() throws Exception {
		UUID empresa = crearEmpresaAprobada("Me");
		AuthTestHelper.Sesion m = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa, Rol.ENCARGADO);
		membresias.guardar(Membresia.crear(m.usuarioId(), empresa, Rol.ENCARGADO));

		mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + m.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.membresiaActiva.empresaId").value(empresa.toString()))
				.andExpect(jsonPath("$.membresiaActiva.rol").value("ENCARGADO"));
	}

	@Test
	void un_cliente_sin_membresia_no_puede_entrar_a_trabajar() throws Exception {
		AuthTestHelper.Sesion c = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);
		mvc.perform(post("/api/v1/auth/contexto").header("Authorization", "Bearer " + c.token())
						.contentType(MediaType.APPLICATION_JSON).content("{\"modo\":\"TRABAJO\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void aceptar_una_invitacion_le_crea_su_membresia() throws Exception {
		UUID empresa = crearEmpresaAprobada("Alta");
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		String correo = "emp-" + UUID.randomUUID() + "@costumi.test";
		String invRes = mvc.perform(post("/api/v1/empleados").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"rol\":\"MOSTRADOR\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String enlace = json.readTree(invRes).get("enlace").asText();

		// La persona acepta (crea su cuenta) y queda logueada; ve su tienda en sus membresías.
		String aceptRes = mvc.perform(post("/api/v1/invitaciones/aceptar").contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"" + enlace + "\",\"password\":\"secret123\",\"aceptaTerminos\":true}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		String tok = json.readTree(aceptRes).get("accessToken").asText();
		mvc.perform(get("/api/v1/auth/me/membresias").header("Authorization", "Bearer " + tok))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.empresaId == '" + empresa + "')].rol", hasItem("MOSTRADOR")));
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/auth/me/membresias")).andExpect(status().isUnauthorized());
	}

	/** POST /auth/contexto con el modo dado; devuelve el accessToken emitido. */
	private String postContexto(String token, String modo) throws Exception {
		String res = mvc.perform(post("/api/v1/auth/contexto").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"modo\":\"" + modo + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andReturn().getResponse().getContentAsString();
		return json.readTree(res).get("accessToken").asText();
	}

	/** Decodifica el payload (claims) de un JWT sin verificar la firma. */
	private JsonNode claimsDe(String jwt) throws Exception {
		return json.readTree(new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[1])));
	}

	private UUID crearEmpresaAprobada(String nombre) throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + " " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		return empresa;
	}
}
