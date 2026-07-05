package com.costumi.backend.clientes;

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

/** Clientes (RF-7): alta, búsqueda, lista negra y aislamiento por tenant. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ClienteIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private UUID crearEmpresa(String nombre) throws Exception {
		String body = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	private String token(UUID empresaId, Rol rol) throws Exception {
		return AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresaId, rol);
	}

	private UUID crearCliente(String tk, String nombre, String documento) throws Exception {
		String body = mvc.perform(post("/api/v1/clientes").header("Authorization", "Bearer " + tk)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\",\"documento\":\"" + documento + "\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	@Test
	void crear_buscar_por_documento_y_ponerlo_en_lista_negra() throws Exception {
		UUID empresa = crearEmpresa("Empresa Cli");
		String mostrador = token(empresa, Rol.MOSTRADOR);
		String doc = "CC-" + UUID.randomUUID();
		UUID cliente = crearCliente(mostrador, "Juan Pérez", doc);

		mvc.perform(get("/api/v1/clientes").param("buscar", doc).header("Authorization", "Bearer " + mostrador))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.documento == '" + doc + "')]").exists());

		String encargado = token(empresa, Rol.ENCARGADO);
		mvc.perform(post("/api/v1/clientes/{id}/lista-negra", cliente).header("Authorization", "Bearer " + encargado)
						.contentType(MediaType.APPLICATION_JSON).content("{\"enListaNegra\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enListaNegra").value(true));
	}

	@Test
	void una_empresa_no_ve_los_clientes_de_otra() throws Exception {
		UUID empresaA = crearEmpresa("Cli A");
		String duenoA = token(empresaA, Rol.DUENO);
		crearCliente(duenoA, "Cliente-A-" + UUID.randomUUID(), "DOC-A");

		UUID empresaB = crearEmpresa("Cli B");
		String duenoB = token(empresaB, Rol.DUENO);
		mvc.perform(get("/api/v1/clientes").header("Authorization", "Bearer " + duenoB))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.documento == 'DOC-A')]").doesNotExist());
	}

	@Test
	void un_rol_sin_permiso_no_puede_crear_cliente_403() throws Exception {
		UUID empresa = crearEmpresa("Cli Bodega");
		String bodega = token(empresa, Rol.BODEGA);

		mvc.perform(post("/api/v1/clientes").header("Authorization", "Bearer " + bodega)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"X\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/clientes")).andExpect(status().isUnauthorized());
	}
}
