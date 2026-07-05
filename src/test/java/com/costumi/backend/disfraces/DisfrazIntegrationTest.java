package com.costumi.backend.disfraces;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Disfraces (RF-2.3/2.4): alta con slots y disponibilidad DERIVADA del stock, acotado al tenant. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DisfrazIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private UUID crearEmpresa(String nombre) throws Exception {
		String body = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	private String duenoDe(UUID empresaId) throws Exception {
		return AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresaId, Rol.DUENO);
	}

	private UUID crearCategoria(String token, String nombre) throws Exception {
		String body = mvc.perform(post("/api/v1/categorias").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	private UUID crearPrenda(String token, UUID categoriaId) throws Exception {
		String body = mvc.perform(post("/api/v1/prendas").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoriaId + "\",\"nombre\":\"Pieza\","
								+ "\"tipoArticulo\":\"RENTA\",\"precioRenta\":40.00}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	private void crearGrupo(String token, UUID prendaId, int cantidad) throws Exception {
		mvc.perform(post("/api/v1/prendas/{prendaId}/grupos-stock", prendaId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"combinacion\":[],\"cantidadInicial\":" + cantidad + "}"))
				.andExpect(status().isCreated());
	}

	private UUID crearUnidadFija(String token, UUID prendaFijaId) throws Exception {
		String body = mvc.perform(post("/api/v1/disfraces").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Traje\",\"modo\":\"UNIDAD_FIJA\",\"prendaFijaId\":\"" + prendaFijaId + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.modo").value("UNIDAD_FIJA"))
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	private boolean disponible(String token, UUID disfrazId) throws Exception {
		String body = mvc.perform(get("/api/v1/disfraces/{id}/disponibilidad", disfrazId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return json.readTree(body).get("disponible").asBoolean();
	}

	@Test
	void disponibilidad_de_unidad_fija_deriva_del_stock() throws Exception {
		String dueno = duenoDe(crearEmpresa("Disfraz Stock"));
		UUID categoria = crearCategoria(dueno, "Traje");

		UUID conStock = crearPrenda(dueno, categoria);
		crearGrupo(dueno, conStock, 3);
		UUID disfrazDisponible = crearUnidadFija(dueno, conStock);

		UUID sinStock = crearPrenda(dueno, categoria);
		crearGrupo(dueno, sinStock, 0);
		UUID disfrazNoDisponible = crearUnidadFija(dueno, sinStock);

		org.assertj.core.api.Assertions.assertThat(disponible(dueno, disfrazDisponible)).isTrue();
		org.assertj.core.api.Assertions.assertThat(disponible(dueno, disfrazNoDisponible)).isFalse();
	}

	@Test
	void por_partes_con_slot_personalizable_deriva_del_pool() throws Exception {
		String dueno = duenoDe(crearEmpresa("Disfraz Pool"));
		UUID categoria = crearCategoria(dueno, "Sombrero");
		UUID prenda = crearPrenda(dueno, categoria);
		crearGrupo(dueno, prenda, 2);

		String body = mvc.perform(post("/api/v1/disfraces").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Pirata\",\"modo\":\"POR_PARTES\",\"slots\":[{\"orden\":1,"
								+ "\"nombre\":\"Sombrero\",\"ejeTalla\":\"LIBRE\",\"ejePrenda\":\"PERSONALIZABLE\","
								+ "\"pool\":{\"categoriaId\":\"" + categoria + "\",\"etiquetasPermitidas\":[]},"
								+ "\"opcional\":false}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.slots[0].ejePrenda").value("PERSONALIZABLE"))
				.andReturn().getResponse().getContentAsString();
		UUID disfraz = UUID.fromString(json.readTree(body).get("id").asText());

		org.assertj.core.api.Assertions.assertThat(disponible(dueno, disfraz)).isTrue();
	}

	@Test
	void por_partes_sin_slots_devuelve_400() throws Exception {
		String dueno = duenoDe(crearEmpresa("Disfraz Vacio"));

		mvc.perform(post("/api/v1/disfraces").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Vacío\",\"modo\":\"POR_PARTES\",\"slots\":[]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void un_rol_sin_permiso_no_puede_crear_disfraz_403() throws Exception {
		UUID empresa = crearEmpresa("Disfraz Rol");
		String mostrador = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.MOSTRADOR);

		mvc.perform(post("/api/v1/disfraces").header("Authorization", "Bearer " + mostrador)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"X\",\"modo\":\"UNIDAD_FIJA\",\"prendaFijaId\":\"" + UUID.randomUUID() + "\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/disfraces")).andExpect(status().isUnauthorized());
	}
}
