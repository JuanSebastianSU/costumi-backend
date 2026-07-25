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

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Horario de atención de la tienda (A7) y carrusel de disfraces destacados (C1). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HorarioYDestacadosIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void el_dueno_fija_el_horario_y_es_publico_en_la_vitrina() throws Exception {
		Tienda t = tiendaActivaConSucursal();

		// Fija lunes 09:00-18:00 y sábado 10:00-14:00.
		mvc.perform(put("/api/v1/empresas/mia/horario").header("Authorization", "Bearer " + t.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"dias\":[{\"diaSemana\":1,\"abre\":\"09:00\",\"cierra\":\"18:00\"},"
								+ "{\"diaSemana\":6,\"abre\":\"10:00\",\"cierra\":\"14:00\"}]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].diaSemana").value(1))
				.andExpect(jsonPath("$[0].abre").value(startsWith("09:00")));

		// GET del propio dueño lo refleja.
		mvc.perform(get("/api/v1/empresas/mia/horario").header("Authorization", "Bearer " + t.dueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));

		// Público en la vitrina (sin token): el cliente ve cuándo abre la tienda.
		mvc.perform(get("/api/v1/marketplace/empresas/{id}/horario", t.empresa()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.diaSemana == 6)].cierra", org.hamcrest.Matchers.hasItem(startsWith("14:00"))));
	}

	@Test
	void un_horario_con_cierre_antes_de_apertura_es_400() throws Exception {
		Tienda t = tiendaActivaConSucursal();
		mvc.perform(put("/api/v1/empresas/mia/horario").header("Authorization", "Bearer " + t.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"dias\":[{\"diaSemana\":1,\"abre\":\"18:00\",\"cierra\":\"09:00\"}]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void el_carrusel_de_destacados_incluye_los_disfraces_de_tiendas_activas() throws Exception {
		Tienda t = tiendaActivaConSucursal();
		UUID categoria = postId("/api/v1/categorias", t.dueno(), "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", t.dueno(), "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Traje\",\"tipoArticulo\":\"VENTA\",\"precioVenta\":90.00}");
		UUID disfraz = postId("/api/v1/disfraces", t.dueno(), "{\"nombre\":\"Catrina Destacada\",\"slots\":[{\"orden\":1,"
				+ "\"nombre\":\"Cuerpo\",\"ejePrenda\":\"FIJA\",\"prendaFijaId\":\"" + prenda + "\",\"opcional\":false}]}");

		// El carrusel público (sin token) trae el disfraz de la tienda activa, con su tienda.
		mvc.perform(get("/api/v1/marketplace/destacados"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.disfrazId == '" + disfraz + "')].nombre",
						org.hamcrest.Matchers.hasItem("Catrina Destacada")))
				.andExpect(jsonPath("$[?(@.disfrazId == '" + disfraz + "')].empresaId",
						org.hamcrest.Matchers.hasItem(t.empresa().toString())));
	}

	private record Tienda(UUID empresa, String dueno) {
	}

	private Tienda tiendaActivaConSucursal() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Hor " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		mvc.perform(post("/api/v1/empresas/{id}/sucursales", empresa).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Centro\"}"))
				.andExpect(status().isCreated());
		return new Tienda(empresa, dueno);
	}

	private UUID postId(String path, String token, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}
}
