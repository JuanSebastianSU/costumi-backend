package com.costumi.backend.inventario;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Subida de foto de prenda (RF-2.9, C1): solo imágenes reales (por magic bytes), no el content-type del cliente. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, PrendaFotoIntegrationTest.AlmacenStubConfig.class})
class PrendaFotoIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	/** Stub del almacén: evita depender de S3; devuelve una URL falsa para probar el flujo hasta el final. */
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

	private UUID crearPrenda() throws Exception {
		UUID empresa = UUID.fromString(json.readTree(mvc.perform(post("/api/v1/empresas")
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Foto " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText());
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID categoria = UUID.fromString(json.readTree(mvc.perform(post("/api/v1/categorias")
						.header("Authorization", "Bearer " + dueno).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText());
		String p = mvc.perform(post("/api/v1/prendas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoria + "\",\"nombre\":\"Traje\",\"tipoArticulo\":\"VENTA\","
								+ "\"precioVenta\":100.00}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		this.dueno = dueno;
		return UUID.fromString(json.readTree(p).get("id").asText());
	}

	private String dueno;

	@Test
	void subir_una_imagen_png_real_funciona() throws Exception {
		UUID prenda = crearPrenda();
		byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
		MockMultipartFile archivo = new MockMultipartFile("archivo", "foto.png", "image/png", png);

		mvc.perform(multipart("/api/v1/prendas/{id}/foto", prenda).file(archivo)
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fotoUrl").value(org.hamcrest.Matchers.endsWith(".png")));
	}

	@Test
	void un_archivo_que_no_es_imagen_devuelve_415_aunque_diga_ser_png() throws Exception {
		UUID prenda = crearPrenda();
		// El cliente miente en el content-type (image/png) pero el contenido no es una imagen -> 415.
		MockMultipartFile falso = new MockMultipartFile("archivo", "malicioso.png", "image/png",
				"<html>no soy una imagen</html>".getBytes());

		mvc.perform(multipart("/api/v1/prendas/{id}/foto", prenda).file(falso)
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isUnsupportedMediaType());
	}
}
