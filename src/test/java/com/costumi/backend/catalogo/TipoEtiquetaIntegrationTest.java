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

/** Motor de etiquetas (RF-2.7): tipos con interruptores + valores, acotados al tenant. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TipoEtiquetaIntegrationTest {

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

	private UUID crearTipo(String token, String nombre, boolean defineVariante, boolean seleccionable) throws Exception {
		String body = mvc.perform(post("/api/v1/tipos-etiqueta")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\",\"defineVariante\":" + defineVariante
								+ ",\"seleccionablePorCliente\":" + seleccionable + "}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	private UUID crearCategoria(String token, String nombre) throws Exception {
		String body = mvc.perform(post("/api/v1/categorias").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	@Test
	void crear_tipo_acotado_a_una_categoria() throws Exception {
		String dueno = duenoDe(crearEmpresa("Empresa Cat"));
		UUID camisas = crearCategoria(dueno, "Camisas");

		String body = mvc.perform(post("/api/v1/tipos-etiqueta").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Cuello\",\"defineVariante\":false,\"seleccionablePorCliente\":false,"
								+ "\"categoriasQueAplica\":[\"" + camisas + "\"]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.categoriasQueAplica[0]").value(camisas.toString()))
				.andReturn().getResponse().getContentAsString();
		UUID tipoId = UUID.fromString(json.readTree(body).get("id").asText());

		mvc.perform(get("/api/v1/tipos-etiqueta").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == '" + tipoId + "')].categoriasQueAplica[0]").value(camisas.toString()));
	}

	@Test
	void crear_tipo_con_categoria_de_otra_empresa_devuelve_400() throws Exception {
		String duenoA = duenoDe(crearEmpresa("Cat A"));
		UUID categoriaDeA = crearCategoria(duenoA, "Camisas");

		String duenoB = duenoDe(crearEmpresa("Cat B"));
		mvc.perform(post("/api/v1/tipos-etiqueta").header("Authorization", "Bearer " + duenoB)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Cuello\",\"defineVariante\":false,\"seleccionablePorCliente\":false,"
								+ "\"categoriasQueAplica\":[\"" + categoriaDeA + "\"]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void crear_tipo_con_interruptores_agregar_valor_y_listar() throws Exception {
		String dueno = duenoDe(crearEmpresa("Empresa Tax"));
		UUID tipoId = crearTipo(dueno, "Color", true, true);

		mvc.perform(get("/api/v1/tipos-etiqueta").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.nombre == 'Color' && @.defineVariante == true)]").exists());

		mvc.perform(post("/api/v1/tipos-etiqueta/{tipoId}/valores", tipoId)
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"valor\":\"Rojo\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.valor").value("Rojo"))
				.andExpect(jsonPath("$.tipoEtiquetaId").value(tipoId.toString()));

		mvc.perform(get("/api/v1/tipos-etiqueta/{tipoId}/valores", tipoId).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.valor == 'Rojo')]").exists());
	}

	@Test
	void agregar_valor_a_un_tipo_de_otra_empresa_devuelve_404() throws Exception {
		String duenoA = duenoDe(crearEmpresa("Empresa A"));
		UUID tipoDeA = crearTipo(duenoA, "Talla", true, false);

		String duenoB = duenoDe(crearEmpresa("Empresa B"));
		mvc.perform(post("/api/v1/tipos-etiqueta/{tipoId}/valores", tipoDeA)
						.header("Authorization", "Bearer " + duenoB)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"valor\":\"M\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void un_rol_sin_permiso_no_puede_crear_tipo_403() throws Exception {
		UUID empresa = crearEmpresa("Empresa Bodega");
		String bodega = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.BODEGA);

		mvc.perform(post("/api/v1/tipos-etiqueta")
						.header("Authorization", "Bearer " + bodega)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Color\",\"defineVariante\":true,\"seleccionablePorCliente\":true}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/tipos-etiqueta")).andExpect(status().isUnauthorized());
	}
}
