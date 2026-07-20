package com.costumi.backend.marketplace;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Etapa 4: el catálogo público del marketplace. Un cliente (sin token) puede ver las prendas de
 * cualquier tienda ACTIVA — datos públicos (nombre, precio, categoría), sin nada privado del negocio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CatalogoPublicoIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private String accessToken(String body) throws Exception {
		return json.readTree(body).get("accessToken").asText();
	}

	@Test
	void el_catalogo_publico_muestra_las_prendas_de_una_tienda_activa() throws Exception {
		String correo = "duenocatalogo-" + UUID.randomUUID() + "@correo.com";

		// Cliente se registra, solicita su tienda y el superadmin la aprueba (ya es Dueño).
		String tokenCliente = accessToken(mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + correo + "\",\"password\":\"clave1234\"}")).andReturn().getResponse().getContentAsString());
		UUID empresaId = UUID.fromString(json.readTree(mvc.perform(post("/api/v1/empresas")
						.header("Authorization", "Bearer " + tokenCliente).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Casa del Terror " + UUID.randomUUID() + "\",\"ubicacion\":\"Centro\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresaId).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		String tokenDueno = accessToken(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + correo + "\",\"password\":\"clave1234\"}")).andReturn().getResponse().getContentAsString());

		// El Dueño crea una categoría y una prenda en su tienda.
		UUID categoriaId = UUID.fromString(json.readTree(mvc.perform(post("/api/v1/categorias")
						.header("Authorization", "Bearer " + tokenDueno).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Terror\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText());
		mvc.perform(post("/api/v1/prendas").header("Authorization", "Bearer " + tokenDueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoriaId + "\",\"tipoArticulo\":\"RENTA\",\"nombre\":\"Vampiro Clásico\",\"precioRenta\":25.00}"))
				.andExpect(status().isCreated());

		// Cualquiera (sin token) ve el catálogo público de la tienda con esa prenda.
		String catalogo = mvc.perform(get("/api/v1/marketplace/empresas/{id}/catalogo", empresaId))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		JsonNode vampiro = null;
		for (JsonNode n : json.readTree(catalogo)) {
			if ("Vampiro Clásico".equals(n.path("nombre").asText())) {
				vampiro = n;
			}
		}
		assertThat(vampiro).as("la prenda debe aparecer en el catálogo público").isNotNull();
		assertThat(vampiro.path("precioRenta").asDouble()).isEqualTo(25.0);
		assertThat(vampiro.path("categoria").asText()).isEqualTo("Terror");
		assertThat(vampiro.path("tipoArticulo").asText()).isEqualTo("RENTA");
	}

	@Test
	void el_catalogo_se_puede_filtrar_por_categoria() throws Exception {
		String correo = "duenofiltro-" + UUID.randomUUID() + "@correo.com";
		String tokenCliente = accessToken(mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + correo + "\",\"password\":\"clave1234\"}")).andReturn().getResponse().getContentAsString());
		UUID empresaId = UUID.fromString(json.readTree(mvc.perform(post("/api/v1/empresas")
						.header("Authorization", "Bearer " + tokenCliente).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Filtro " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresaId).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		String dueno = accessToken(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + correo + "\",\"password\":\"clave1234\"}")).andReturn().getResponse().getContentAsString());

		UUID catTerror = crearCategoria(dueno, "Terror");
		UUID catFantasia = crearCategoria(dueno, "Fantasia");
		crearPrenda(dueno, catTerror, "Vampiro");
		crearPrenda(dueno, catFantasia, "Hada");

		// Filtrando por Terror, solo aparece Vampiro; no aparece Hada (RF-18.1).
		mvc.perform(get("/api/v1/marketplace/empresas/{id}/catalogo", empresaId).param("categoria", catTerror.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.nombre == 'Vampiro')]").exists())
				.andExpect(jsonPath("$[?(@.nombre == 'Hada')]").doesNotExist());
	}

	private UUID crearCategoria(String dueno, String nombre) throws Exception {
		return UUID.fromString(json.readTree(mvc.perform(post("/api/v1/categorias")
						.header("Authorization", "Bearer " + dueno).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + " " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText());
	}

	private void crearPrenda(String dueno, UUID categoriaId, String nombre) throws Exception {
		mvc.perform(post("/api/v1/prendas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoriaId + "\",\"tipoArticulo\":\"RENTA\",\"nombre\":\""
								+ nombre + "\",\"precioRenta\":25.00}"))
				.andExpect(status().isCreated());
	}

	@Test
	void el_catalogo_de_una_empresa_inexistente_o_no_activa_viene_vacio() throws Exception {
		mvc.perform(get("/api/v1/marketplace/empresas/{id}/catalogo", UUID.randomUUID()))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.length()").value(0));
	}
}
