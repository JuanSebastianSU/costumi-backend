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

/**
 * El dueño puede leer su propia tienda (RF-15.1). Antes no existía forma: el único listado de empresas
 * es del SuperAdmin, así que la app no podía mostrar ni el nombre de la tienda en Gestión.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MiEmpresaIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void el_dueno_ve_el_nombre_de_su_tienda() throws Exception {
		String nombre = "Tienda " + UUID.randomUUID();
		UUID empresa = empresaAprobada(nombre);
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);

		mvc.perform(get("/api/v1/empresas/mia").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(empresa.toString()))
				.andExpect(jsonPath("$.nombre").value(nombre))
				.andExpect(jsonPath("$.estado").value("ACTIVA"));
	}

	/** Cada uno ve la suya: la empresa sale del token, no de la ruta, así que no hay forma de pedir otra. */
	@Test
	void cada_empresa_ve_solamente_la_suya() throws Exception {
		UUID empresaA = empresaAprobada("Tienda A " + UUID.randomUUID());
		UUID empresaB = empresaAprobada("Tienda B " + UUID.randomUUID());
		String duenoDeB = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresaB, Rol.DUENO);

		mvc.perform(get("/api/v1/empresas/mia").header("Authorization", "Bearer " + duenoDeB))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(empresaB.toString()))
				.andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(empresaA.toString())));
	}

	/** Un CLIENTE del marketplace no pertenece a ninguna tienda: 403, no un 500 ni la tienda de otro. */
	@Test
	void un_usuario_sin_empresa_no_tiene_tienda_propia() throws Exception {
		String cliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);

		mvc.perform(get("/api/v1/empresas/mia").header("Authorization", "Bearer " + cliente))
				.andExpect(status().isForbidden());
	}

	@Test
	void sin_token_no_se_puede_consultar() throws Exception {
		mvc.perform(get("/api/v1/empresas/mia")).andExpect(status().isUnauthorized());
	}

	private UUID empresaAprobada(String nombre) throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		return empresa;
	}
}
