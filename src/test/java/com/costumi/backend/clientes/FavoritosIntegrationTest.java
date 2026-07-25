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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** "Mis guardados" del cliente (C4): guardar/quitar favoritos, idempotente y por el usuario del token. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FavoritosIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private String cliente() throws Exception {
		return AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);
	}

	private String cuerpo(UUID disfraz, String nombre) {
		return "{\"disfrazId\":\"" + disfraz + "\",\"empresaId\":\"" + UUID.randomUUID() + "\",\"nombre\":\"" + nombre
				+ "\",\"fotoUrl\":\"https://cdn/x.png\",\"precioRenta\":20.00,\"precioVenta\":80.00}";
	}

	@Test
	void guardar_listar_y_quitar_un_favorito() throws Exception {
		String cli = "Bearer " + cliente();
		UUID disfraz = UUID.randomUUID();

		mvc.perform(post("/api/v1/clientes/me/favoritos").header("Authorization", cli)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo(disfraz, "Catrina")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.disfrazId").value(disfraz.toString()))
				.andExpect(jsonPath("$.nombre").value("Catrina"));

		mvc.perform(get("/api/v1/clientes/me/favoritos").header("Authorization", cli))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].disfrazId").value(disfraz.toString()));

		mvc.perform(delete("/api/v1/clientes/me/favoritos/{id}", disfraz).header("Authorization", cli))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/v1/clientes/me/favoritos").header("Authorization", cli))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void guardar_el_mismo_disfraz_dos_veces_no_duplica() throws Exception {
		String cli = "Bearer " + cliente();
		UUID disfraz = UUID.randomUUID();

		mvc.perform(post("/api/v1/clientes/me/favoritos").header("Authorization", cli)
				.contentType(MediaType.APPLICATION_JSON).content(cuerpo(disfraz, "Catrina"))).andExpect(status().isOk());
		// Re-guardar (con snapshot nuevo) actualiza, no crea otra fila.
		mvc.perform(post("/api/v1/clientes/me/favoritos").header("Authorization", cli)
				.contentType(MediaType.APPLICATION_JSON).content(cuerpo(disfraz, "Catrina Deluxe")))
				.andExpect(status().isOk());

		mvc.perform(get("/api/v1/clientes/me/favoritos").header("Authorization", cli))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].nombre").value("Catrina Deluxe"));
	}

	@Test
	void un_cliente_no_ve_los_favoritos_de_otro() throws Exception {
		String uno = "Bearer " + cliente();
		mvc.perform(post("/api/v1/clientes/me/favoritos").header("Authorization", uno)
				.contentType(MediaType.APPLICATION_JSON).content(cuerpo(UUID.randomUUID(), "Suyo")))
				.andExpect(status().isOk());

		String otro = "Bearer " + cliente();
		mvc.perform(get("/api/v1/clientes/me/favoritos").header("Authorization", otro))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/clientes/me/favoritos")).andExpect(status().isUnauthorized());
	}
}
