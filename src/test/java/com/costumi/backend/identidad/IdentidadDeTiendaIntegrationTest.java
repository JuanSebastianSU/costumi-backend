package com.costumi.backend.identidad;

import com.costumi.backend.TestcontainersConfiguration;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Identidad y media de la tienda (A7): el Dueño edita su tienda y sube logo/portada; se refleja en la vitrina. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, IdentidadDeTiendaIntegrationTest.AlmacenStubConfig.class})
class IdentidadDeTiendaIntegrationTest {

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

	private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

	@Test
	void el_dueno_edita_su_tienda_sube_logo_y_se_ve_en_la_vitrina() throws Exception {
		String dueno = tiendaActivaConSucursal();

		// Editar identidad: descripción + ciudad + ubicación.
		mvc.perform(patch("/api/v1/empresas/mia").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Mi Tienda\",\"descripcion\":\"Disfraces para todos\","
								+ "\"ciudad\":\"Pasto\",\"ubicacion\":\"Centro\",\"contacto\":\"3001234567\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.descripcion").value("Disfraces para todos"))
				.andExpect(jsonPath("$.ciudad").value("Pasto"));

		// Subir el logo (PNG real por magic bytes).
		mvc.perform(multipart("/api/v1/empresas/mia/logo").file(new MockMultipartFile("archivo", "l.png", "image/png", PNG))
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.logoUrl").value(org.hamcrest.Matchers.endsWith(".png")));

		// GET /mia refleja todo.
		mvc.perform(get("/api/v1/empresas/mia").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ciudad").value("Pasto"))
				.andExpect(jsonPath("$.logoUrl").value(org.hamcrest.Matchers.endsWith(".png")));

		// La vitrina pública del marketplace muestra la marca (logo/ciudad/descripción).
		mvc.perform(get("/api/v1/marketplace/empresas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.nombre == 'Mi Tienda')].ciudad").value(org.hamcrest.Matchers.hasItem("Pasto")))
				.andExpect(jsonPath("$[?(@.nombre == 'Mi Tienda')].logoUrl",
						org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.endsWith(".png"))));
	}

	@Test
	void un_archivo_que_no_es_imagen_devuelve_415() throws Exception {
		String dueno = tiendaActivaConSucursal();
		mvc.perform(multipart("/api/v1/empresas/mia/logo")
						.file(new MockMultipartFile("archivo", "x.png", "image/png", "<html>no</html>".getBytes()))
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isUnsupportedMediaType());
	}

	/** Crea una empresa, la aprueba, le da un Dueño y una sucursal (para que salga en el marketplace). */
	private String tiendaActivaConSucursal() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Prov " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		mvc.perform(post("/api/v1/empresas/{id}/sucursales", empresa).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Centro\"}"))
				.andExpect(status().isCreated());
		return dueno;
	}
}
