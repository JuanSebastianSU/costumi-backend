package com.costumi.backend.disfraces;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.identidad.AuthTestHelper;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import com.costumi.backend.inventario.aplicacion.AlmacenDeImagenes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Foto del disfraz (RF-2.9/18.3): el dueño la sube y el cliente la ve en la vitrina. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, DisfrazFotoIntegrationTest.AlmacenStubConfig.class})
class DisfrazFotoIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	/** Stub del almacén (igual que en la foto de prenda): evita depender de S3, devuelve una URL falsa. */
	@TestConfiguration
	static class AlmacenStubConfig {
		@Bean
		@Primary
		AlmacenDeImagenes almacenStub() {
			return new AlmacenDeImagenes() {
				@Override
				public boolean disponible() {
					return true;
				}

				@Override
				public String subir(byte[] contenido, String contentType, String clave) {
					return "https://cdn.test/" + clave;
				}
			};
		}
	}

	private UUID postId(String path, String tk, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + tk)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	@Test
	void el_dueno_sube_la_foto_del_disfraz_y_el_cliente_la_ve_en_la_vitrina() throws Exception {
		UUID empresa = UUID.fromString(json.readTree(mvc.perform(post("/api/v1/empresas")
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"DisfrazFoto " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText());
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Traje\",\"tipoArticulo\":\"RENTA\",\"precioRenta\":40.00}");
		UUID disfraz = postId("/api/v1/disfraces", dueno, "{\"nombre\":\"Pirata\",\"slots\":[{\"orden\":1,"
				+ "\"nombre\":\"Traje\",\"ejePrenda\":\"FIJA\",\"prendaFijaId\":\"" + prenda + "\",\"opcional\":false}]}");

		// El dueño sube la foto (PNG real por magic bytes).
		byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
		MockMultipartFile archivo = new MockMultipartFile("archivo", "disfraz.png", "image/png", png);
		mvc.perform(multipart("/api/v1/disfraces/{id}/foto", disfraz).file(archivo)
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fotoUrl").value(org.hamcrest.Matchers.endsWith(".png")));

		// El cliente (sin token) ve el disfraz en la vitrina CON su foto.
		mvc.perform(get("/api/v1/marketplace/empresas/{empresaId}/disfraces", empresa))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == '" + disfraz + "')].fotoUrl")
						.value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.endsWith(".png"))));
	}

	@Test
	void la_ruleta_de_un_slot_personalizable_muestra_la_foto_de_cada_prenda() throws Exception {
		UUID empresa = UUID.fromString(json.readTree(mvc.perform(post("/api/v1/empresas")
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Ruleta " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"Centro\"}");
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Sombrero\",\"tipoArticulo\":\"RENTA\",\"precioRenta\":40.00}");
		// Foto de la prenda + stock (para que aparezca disponible en la ruleta).
		byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
		mvc.perform(multipart("/api/v1/prendas/{id}/foto", prenda)
						.file(new MockMultipartFile("archivo", "p.png", "image/png", png))
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk());
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno,
				"{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":3}");
		// Disfraz con un slot personalizable cuyo pool es esa categoría.
		UUID disfraz = postId("/api/v1/disfraces", dueno, "{\"nombre\":\"Pirata\",\"slots\":[{\"orden\":1,"
				+ "\"nombre\":\"Sombrero\",\"ejePrenda\":\"PERSONALIZABLE\",\"pool\":{\"categoriaId\":\"" + categoria
				+ "\",\"etiquetasPermitidas\":[]},\"opcional\":false}]}");

		// La ruleta del slot devuelve la opción CON su foto (para que el cliente la vea al elegir).
		mvc.perform(get("/api/v1/marketplace/empresas/{e}/disfraces/{d}/slots/{o}/opciones", empresa, disfraz, 1))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.opciones[?(@.prendaId == '" + prenda + "')].fotoUrl")
						.value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.endsWith(".png"))));
	}
}
