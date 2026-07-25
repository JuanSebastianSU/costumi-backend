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

/** Media y ubicación de la sucursal (A7/G17): el Dueño la edita y sube su foto; se refleja en la vitrina. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, SucursalMediaIntegrationTest.AlmacenStubConfig.class})
class SucursalMediaIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

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
	void edita_ubicacion_y_foto_de_la_sucursal_y_se_ve_en_la_vitrina() throws Exception {
		Escenario e = tiendaConSucursal();

		mvc.perform(patch("/api/v1/empresas/{empresaId}/sucursales/{id}", e.empresa(), e.sucursal())
						.header("Authorization", "Bearer " + e.dueno()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Centro\",\"descripcion\":\"Frente al parque\","
								+ "\"latitud\":1.2136,\"longitud\":-77.2811}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.descripcion").value("Frente al parque"))
				.andExpect(jsonPath("$.latitud").value(1.2136))
				.andExpect(jsonPath("$.longitud").value(-77.2811));

		mvc.perform(multipart("/api/v1/empresas/{empresaId}/sucursales/{id}/foto", e.empresa(), e.sucursal())
						.file(new MockMultipartFile("archivo", "s.png", "image/png", PNG))
						.header("Authorization", "Bearer " + e.dueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fotoUrl").value(org.hamcrest.Matchers.endsWith(".png")));

		// La vitrina pública del punto de retiro muestra coordenadas y foto (para el mapa del cliente).
		mvc.perform(get("/api/v1/marketplace/empresas/{empresaId}/sucursales", e.empresa()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].latitud").value(1.2136))
				.andExpect(jsonPath("$[0].fotoUrl").value(org.hamcrest.Matchers.endsWith(".png")));
	}

	@Test
	void una_coordenada_fuera_de_rango_devuelve_400() throws Exception {
		Escenario e = tiendaConSucursal();
		mvc.perform(patch("/api/v1/empresas/{empresaId}/sucursales/{id}", e.empresa(), e.sucursal())
						.header("Authorization", "Bearer " + e.dueno()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Centro\",\"latitud\":999.0}"))
				.andExpect(status().isBadRequest());
	}

	private record Escenario(UUID empresa, UUID sucursal, String dueno) {
	}

	private Escenario tiendaConSucursal() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Suc " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		String s = mvc.perform(post("/api/v1/empresas/{id}/sucursales", empresa).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Centro\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID sucursal = UUID.fromString(json.readTree(s).get("id").asText());
		return new Escenario(empresa, sucursal, dueno);
	}
}
