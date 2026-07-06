package com.costumi.backend.marketplace;

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

/** Marketplace (RF-18.1/RF-15.6): solo las empresas ACTIVAS son visibles públicamente. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MarketplaceIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private UUID crearEmpresa(String nombre) throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	@Test
	void solo_las_empresas_activas_aparecen_en_la_vitrina() throws Exception {
		String activa = "Activa-" + UUID.randomUUID();
		String pendiente = "Pendiente-" + UUID.randomUUID();
		UUID empresaActiva = crearEmpresa(activa);
		crearEmpresa(pendiente); // queda PENDIENTE

		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresaActiva)
						.header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());

		// Endpoint público: sin token.
		mvc.perform(get("/api/v1/marketplace/empresas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.nombre == '" + activa + "')]").exists())
				.andExpect(jsonPath("$[?(@.nombre == '" + pendiente + "')]").doesNotExist());
	}

	@Test
	void la_vitrina_se_puede_buscar_por_texto() throws Exception {
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		String marca = "Disfraces" + UUID.randomUUID().toString().substring(0, 8);
		UUID coincide = crearEmpresa(marca + " Centro");
		UUID otra = crearEmpresa("Trajes " + UUID.randomUUID());
		for (UUID e : new UUID[] { coincide, otra }) {
			mvc.perform(post("/api/v1/empresas/{id}/aprobar", e).header("Authorization", "Bearer " + superAdmin))
					.andExpect(status().isOk());
		}

		// Búsqueda por texto (RF-18.1): solo la que coincide en el nombre.
		mvc.perform(get("/api/v1/marketplace/empresas").param("buscar", marca))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.nombre == '" + marca + " Centro')]").exists())
				.andExpect(jsonPath("$.length()").value(1));
	}
}
