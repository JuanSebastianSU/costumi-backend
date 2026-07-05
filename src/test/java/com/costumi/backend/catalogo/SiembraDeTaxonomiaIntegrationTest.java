package com.costumi.backend.catalogo;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.identidad.AuthTestHelper;
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

/** Al aprobar una empresa se siembra la taxonomía básica (RF-2.7.7 / RF-13.5). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SiembraDeTaxonomiaIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void aprobar_una_empresa_siembra_categorias_y_tipos_basicos() throws Exception {
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);

		String empresaBody = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Empresa Sembrada\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresaId = UUID.fromString(json.readTree(empresaBody).get("id").asText());

		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresaId).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());

		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresaId, Rol.DUENO);

		mvc.perform(get("/api/v1/categorias").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.nombre == 'Sombrero')]").exists())
				.andExpect(jsonPath("$[?(@.nombre == 'Accesorio')]").exists());

		mvc.perform(get("/api/v1/tipos-etiqueta").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.nombre == 'Color' && @.defineVariante == true)]").exists())
				.andExpect(jsonPath("$[?(@.nombre == 'Talla')]").exists());
	}

	@Test
	void una_empresa_pendiente_no_tiene_taxonomia_sembrada() throws Exception {
		// Las empresas creadas para pruebas no se aprueban -> no se siembra (evita colisiones de nombres).
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder,
				crearEmpresaPendiente("Sin sembrar"), Rol.DUENO);

		mvc.perform(get("/api/v1/categorias").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.nombre == 'Sombrero')]").doesNotExist());
	}

	private UUID crearEmpresaPendiente(String nombre) throws Exception {
		String body = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}
}
