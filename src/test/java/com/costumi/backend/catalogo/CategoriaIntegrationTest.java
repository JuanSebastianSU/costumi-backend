package com.costumi.backend.catalogo;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.identidad.AuthTestHelper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Categorías (RF-2.8) con aislamiento multi-tenant: cada empresa solo ve lo suyo. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CategoriaIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private UUID crearEmpresa(String nombre) throws Exception {
		String body = mvc.perform(post("/api/v1/empresas")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	private String duenoDe(UUID empresaId) throws Exception {
		return AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresaId, Rol.DUENO);
	}

	@Test
	void el_dueno_crea_y_lista_su_categoria() throws Exception {
		UUID empresa = crearEmpresa("Empresa Cat");
		String dueno = duenoDe(empresa);

		mvc.perform(post("/api/v1/categorias")
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Camisa\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nombre").value("Camisa"))
				.andExpect(jsonPath("$.empresaId").value(empresa.toString()));

		mvc.perform(get("/api/v1/categorias").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.nombre == 'Camisa')]").exists());
	}

	@Test
	void una_empresa_no_ve_las_categorias_de_otra() throws Exception {
		UUID empresaA = crearEmpresa("Empresa A");
		String duenoA = duenoDe(empresaA);
		String nombreExclusivo = "SoloA-" + UUID.randomUUID();
		mvc.perform(post("/api/v1/categorias")
						.header("Authorization", "Bearer " + duenoA)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombreExclusivo + "\"}"))
				.andExpect(status().isCreated());

		UUID empresaB = crearEmpresa("Empresa B");
		String duenoB = duenoDe(empresaB);
		String body = mvc.perform(get("/api/v1/categorias").header("Authorization", "Bearer " + duenoB))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		JsonNode lista = json.readTree(body);
		boolean veLaAjena = false;
		for (JsonNode nodo : lista) {
			if (nombreExclusivo.equals(nodo.get("nombre").asText())) {
				veLaAjena = true;
			}
		}
		assertThat(veLaAjena).isFalse();
	}

	@Test
	void crear_una_categoria_con_nombre_duplicado_devuelve_409_no_500() throws Exception {
		UUID empresa = crearEmpresa("Empresa Dup");
		String dueno = duenoDe(empresa);
		String nombre = "Duplicada-" + UUID.randomUUID();
		mvc.perform(post("/api/v1/categorias").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated());
		// El mismo nombre choca con el índice único: debe ser 409 (no un 500 crudo).
		mvc.perform(post("/api/v1/categorias").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/categorias")).andExpect(status().isUnauthorized());
	}

	@Test
	void un_rol_sin_permiso_no_puede_crear_categoria_403() throws Exception {
		UUID empresa = crearEmpresa("Empresa Mostrador");
		String mostrador = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.MOSTRADOR);

		mvc.perform(post("/api/v1/categorias")
						.header("Authorization", "Bearer " + mostrador)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Camisa\"}"))
				.andExpect(status().isForbidden());
	}
}
