package com.costumi.backend.identidad;

import com.costumi.backend.TestcontainersConfiguration;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Solicitud de tienda (marketplace, Etapa 2): un CLIENTE autenticado registra su tienda con
 * ubicación/contacto; queda PENDIENTE ligada a él y aparece en el panel del SuperAdmin con esos datos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SolicitudDeTiendaIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private String tokenCliente(String email) throws Exception {
		String body = mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"clave1234\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return json.readTree(body).get("accessToken").asText();
	}

	@Test
	void un_cliente_solicita_su_tienda_y_el_superadmin_la_ve_con_ubicacion_contacto_y_solicitante() throws Exception {
		String correo = "duenopotencial-" + UUID.randomUUID() + "@correo.com";
		String tokenCliente = tokenCliente(correo);

		String miId = json.readTree(mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + tokenCliente))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asText();

		String nombreTienda = "Disfraces " + UUID.randomUUID();
		mvc.perform(post("/api/v1/empresas").header("Authorization", "Bearer " + tokenCliente)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombreTienda + "\",\"ubicacion\":\"Calle Falsa 123\",\"contacto\":\"3110000000\"}"))
				.andExpect(status().isCreated());

		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		String pendientes = mvc.perform(get("/api/v1/empresas/pendientes").header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		JsonNode mia = null;
		for (JsonNode n : json.readTree(pendientes)) {
			if (nombreTienda.equals(n.path("nombre").asText())) {
				mia = n;
			}
		}
		assertThat(mia).as("la solicitud del cliente debe aparecer en el panel del superadmin").isNotNull();
		assertThat(mia.path("ubicacion").asText()).isEqualTo("Calle Falsa 123");
		assertThat(mia.path("contacto").asText()).isEqualTo("3110000000");
		assertThat(mia.path("solicitanteId").asText()).isEqualTo(miId);
	}

	@Test
	void el_registro_sin_token_sigue_funcionando_sin_solicitante() throws Exception {
		String nombreTienda = "Tienda Anonima " + UUID.randomUUID();
		mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombreTienda + "\"}"))
				.andExpect(status().isCreated());
	}
}
