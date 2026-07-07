package com.costumi.backend.identidad;

import com.costumi.backend.TestcontainersConfiguration;
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

/**
 * Etapa 3: cuando el SuperAdmin aprueba una solicitud de tienda, el cliente solicitante se
 * promueve a DUEÑO de la nueva empresa (misma cuenta) y la empresa queda lista para operar
 * (sucursal "Casa Matriz"). Al re-loguearse, el cliente ya es DUEÑO con su empresa.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AprobacionPromueveDuenoIntegrationTest {

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
	void aprobar_la_solicitud_promueve_al_cliente_a_dueno_y_crea_casa_matriz() throws Exception {
		String correo = "futurodueno-" + UUID.randomUUID() + "@correo.com";

		// 1) El cliente se registra (queda logueado como CLIENTE sin empresa).
		String tokenCliente = accessToken(mvc.perform(post("/api/v1/auth/registro")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"password\":\"clave1234\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

		// 2) Solicita su tienda (con token: queda como solicitante).
		String empresaBody = mvc.perform(post("/api/v1/empresas").header("Authorization", "Bearer " + tokenCliente)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Mi Tienda " + UUID.randomUUID() + "\",\"ubicacion\":\"Av. Siempre Viva 742\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresaId = UUID.fromString(json.readTree(empresaBody).get("id").asText());

		// 3) El SuperAdmin aprueba.
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresaId).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());

		// 4) El cliente re-inicia sesión: ahora es DUEÑO de SU empresa.
		String tokenDueno = accessToken(mvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"password\":\"clave1234\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

		mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + tokenDueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rol").value("DUENO"))
				.andExpect(jsonPath("$.empresaId").value(empresaId.toString()));

		// 5) Su empresa quedó operativa con la sucursal "Casa Matriz".
		mvc.perform(get("/api/v1/empresas/{id}/sucursales", empresaId).header("Authorization", "Bearer " + tokenDueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].nombre").value("Casa Matriz"));
	}

	@Test
	void aprobar_un_registro_sin_solicitante_no_promueve_a_nadie() throws Exception {
		// Registro anónimo (sin token): no hay solicitante, solo se activa la empresa.
		String empresaBody = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Sin Solicitante " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresaId = UUID.fromString(json.readTree(empresaBody).get("id").asText());

		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresaId).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("ACTIVA"));
	}
}
