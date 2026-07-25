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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Foto de perfil del usuario (A7-iii): la sube su propia cuenta y queda en su perfil. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, PerfilFotoIntegrationTest.AlmacenStubConfig.class})
class PerfilFotoIntegrationTest {

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
	void el_usuario_sube_su_foto_y_queda_en_su_perfil() throws Exception {
		String cliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);

		mvc.perform(multipart("/api/v1/perfil/foto")
						.file(new MockMultipartFile("archivo", "yo.png", "image/png", PNG))
						.header("Authorization", "Bearer " + cliente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fotoUrl").value(org.hamcrest.Matchers.endsWith(".png")));

		mvc.perform(get("/api/v1/perfil").header("Authorization", "Bearer " + cliente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fotoUrl").value(org.hamcrest.Matchers.endsWith(".png")));
	}

	@Test
	void un_archivo_que_no_es_imagen_devuelve_415() throws Exception {
		String cliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);
		mvc.perform(multipart("/api/v1/perfil/foto")
						.file(new MockMultipartFile("archivo", "x.png", "image/png", "no soy imagen".getBytes()))
						.header("Authorization", "Bearer " + cliente))
				.andExpect(status().isUnsupportedMediaType());
	}
}
