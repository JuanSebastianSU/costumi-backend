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

/** Membresías multi-tienda y cambio de contexto (Fase B). */
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
	void el_usuario_ve_sus_tiendas_y_cambia_de_contexto() throws Exception {
		UUID empresaA = crearEmpresaAprobada("A");
		UUID empresaB = crearEmpresaAprobada("B");
		AuthTestHelper.Sesion m = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresaA, Rol.MOSTRADOR);
		// Es MOSTRADOR en A y ENCARGADO en B.
		membresias.guardar(Membresia.crear(m.usuarioId(), empresaA, Rol.MOSTRADOR));
		membresias.guardar(Membresia.crear(m.usuarioId(), empresaB, Rol.ENCARGADO));

		// Ve sus dos tiendas con su rol en cada una.
		mvc.perform(get("/api/v1/auth/me/membresias").header("Authorization", "Bearer " + m.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[?(@.empresaId == '" + empresaA + "')].rol", hasItem("MOSTRADOR")))
				.andExpect(jsonPath("$[?(@.empresaId == '" + empresaB + "')].rol", hasItem("ENCARGADO")));

		// Cambia de contexto a B → token con empresa B + rol ENCARGADO.
		String res = mvc.perform(post("/api/v1/auth/contexto").header("Authorization", "Bearer " + m.token())
						.contentType(MediaType.APPLICATION_JSON).content("{\"empresaId\":\"" + empresaB + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andReturn().getResponse().getContentAsString();
		JsonNode claims = json.readTree(new String(Base64.getUrlDecoder().decode(
				json.readTree(res).get("accessToken").asText().split("\\.")[1])));
		assertThat(claims.get("empresa_id").asText()).isEqualTo(empresaB.toString());
		assertThat(claims.get("rol").asText()).isEqualTo("ENCARGADO");

		// A una tienda donde no tiene membresía → 400.
		mvc.perform(post("/api/v1/auth/contexto").header("Authorization", "Bearer " + m.token())
						.contentType(MediaType.APPLICATION_JSON).content("{\"empresaId\":\"" + UUID.randomUUID() + "\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void dar_de_alta_un_empleado_le_crea_su_membresia() throws Exception {
		UUID empresa = crearEmpresaAprobada("Alta");
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		String correo = "emp-" + UUID.randomUUID() + "@costumi.test";
		mvc.perform(post("/api/v1/empleados").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"password\":\"secret123\",\"rol\":\"MOSTRADOR\"}"))
				.andExpect(status().isCreated());

		// El empleado inicia sesión y ve su tienda en sus membresías.
		String login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"password\":\"secret123\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		String tok = json.readTree(login).get("accessToken").asText();
		mvc.perform(get("/api/v1/auth/me/membresias").header("Authorization", "Bearer " + tok))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.empresaId == '" + empresa + "')].rol", hasItem("MOSTRADOR")));
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/auth/me/membresias")).andExpect(status().isUnauthorized());
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
