package com.costumi.backend.disfraces;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Categorías de disfraz (RF-2.3): taxonomía propia, separada de las categorías de prenda. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CategoriaDeDisfrazIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private String montarDueno() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"CatDisfraz " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		return AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
	}

	private UUID crearCategoria(String dueno, String nombre) throws Exception {
		String res = mvc.perform(post("/api/v1/disfraces/categorias").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	@Test
	void crear_listar_renombrar_y_archivar_categoria_de_disfraz() throws Exception {
		String dueno = montarDueno();
		UUID piratas = crearCategoria(dueno, "Piratas");

		mvc.perform(get("/api/v1/disfraces/categorias").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == '" + piratas + "')].nombre").value("Piratas"));

		mvc.perform(patch("/api/v1/disfraces/categorias/{id}", piratas).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Piratas del Caribe\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Piratas del Caribe"));

		mvc.perform(post("/api/v1/disfraces/categorias/{id}/archivar", piratas).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.archivada").value(true));
	}

	@Test
	void un_disfraz_puede_llevar_una_categoria_de_disfraz_y_filtrar_por_ella() throws Exception {
		String dueno = montarDueno();
		UUID piratas = crearCategoria(dueno, "Piratas");
		UUID categoriaPrenda = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Camisa " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoriaPrenda
				+ "\",\"nombre\":\"Camisa\",\"tipoArticulo\":\"AMBOS\",\"precioRenta\":30.00,\"precioVenta\":90.00}");

		String cuerpo = "{\"nombre\":\"Pirata Clásico\",\"categoriaId\":\"" + piratas + "\",\"slots\":["
				+ "{\"orden\":1,\"nombre\":\"Cuerpo\",\"ejePrenda\":\"FIJA\",\"prendaFijaId\":\"" + prenda
				+ "\",\"opcional\":false}]}";
		mvc.perform(post("/api/v1/disfraces").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.categoriaId").value(piratas.toString()));

		// Filtra la lista por esa categoría de disfraz.
		mvc.perform(get("/api/v1/disfraces").param("categoriaId", piratas.toString())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido.length()").value(1))
				.andExpect(jsonPath("$.contenido[0].nombre").value("Pirata Clásico"));
	}

	@Test
	void una_categoria_de_PRENDA_no_sirve_como_categoria_de_disfraz() throws Exception {
		String dueno = montarDueno();
		UUID categoriaPrenda = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Camisa " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoriaPrenda
				+ "\",\"nombre\":\"Camisa\",\"tipoArticulo\":\"AMBOS\",\"precioRenta\":30.00,\"precioVenta\":90.00}");

		// Usar el id de una categoría de PRENDA como categoría del disfraz debe fallar (400).
		String cuerpo = "{\"nombre\":\"Malo\",\"categoriaId\":\"" + categoriaPrenda + "\",\"slots\":["
				+ "{\"orden\":1,\"nombre\":\"Cuerpo\",\"ejePrenda\":\"FIJA\",\"prendaFijaId\":\"" + prenda
				+ "\",\"opcional\":false}]}";
		mvc.perform(post("/api/v1/disfraces").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isBadRequest());
	}

	private UUID postId(String path, String token, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}
}
