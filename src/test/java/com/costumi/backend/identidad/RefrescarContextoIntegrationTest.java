package com.costumi.backend.identidad;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.identidad.dominio.Membresia;
import com.costumi.backend.identidad.dominio.MembresiaRepository;
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

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** El modo Comprando/Trabajando (H1) sobrevive al refresh del token (Fase B). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RefrescarContextoIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	MembresiaRepository membresias;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void el_modo_comprando_sobrevive_al_refresh() throws Exception {
		String token = sesionConMembresia(crearEmpresaAprobada());
		JsonNode claims = claimsDe(refrescar(refreshDeContexto(token, "COMPRA")));
		assertThat(claims.get("rol").asText()).isEqualTo("CLIENTE");
		assertThat(claims.has("empresa_id")).isFalse();
	}

	@Test
	void el_modo_trabajando_sobrevive_al_refresh() throws Exception {
		UUID empresa = crearEmpresaAprobada();
		String token = sesionConMembresia(empresa);
		JsonNode claims = claimsDe(refrescar(refreshDeContexto(token, "TRABAJO")));
		assertThat(claims.get("rol").asText()).isEqualTo("MOSTRADOR");
		assertThat(claims.get("empresa_id").asText()).isEqualTo(empresa.toString());
	}

	// --- helpers ---

	/** Siembra un MOSTRADOR con su membresía activa en la empresa y devuelve su token de sesión. */
	private String sesionConMembresia(UUID empresa) throws Exception {
		AuthTestHelper.Sesion m = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa, Rol.MOSTRADOR);
		membresias.guardar(Membresia.crear(m.usuarioId(), empresa, Rol.MOSTRADOR));
		return m.token();
	}

	/** Cambia de contexto y devuelve el refreshToken emitido. */
	private String refreshDeContexto(String token, String modo) throws Exception {
		String res = mvc.perform(post("/api/v1/auth/contexto").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"modo\":\"" + modo + "\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return json.readTree(res).get("refreshToken").asText();
	}

	/** Refresca y devuelve el nuevo accessToken. */
	private String refrescar(String refreshToken) throws Exception {
		String res = mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + refreshToken + "\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return json.readTree(res).get("accessToken").asText();
	}

	private JsonNode claimsDe(String jwt) throws Exception {
		return json.readTree(new String(Base64.getUrlDecoder().decode(jwt.split("\\.")[1])));
	}

	private UUID crearEmpresaAprobada() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Ref " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		return empresa;
	}
}
