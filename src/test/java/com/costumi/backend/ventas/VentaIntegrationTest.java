package com.costumi.backend.ventas;

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

/** Ventas/POS (RF-4): total con descuento, a nombre del empleado del token. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VentaIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private String dueno;

	private UUID postId(String path, String token, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	private UUID[] montar() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Venta " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		this.dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"Centro\"}");
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Peluca " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Peluca\",\"tipoArticulo\":\"VENTA\",\"precioVenta\":50.00}");
		return new UUID[] { sucursal, prenda };
	}

	@Test
	void registrar_venta_calcula_el_total_con_descuento() throws Exception {
		UUID[] ctx = montar();
		UUID sucursal = ctx[0];
		UUID prenda = ctx[1];

		mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"descuento\":10.00,\"lineas\":[{\"prendaId\":\""
								+ prenda + "\",\"cantidad\":2,\"precioUnitario\":50.00}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.total").value(90.00))
				.andExpect(jsonPath("$.estado").value("CONFIRMADA"))
				.andExpect(jsonPath("$.lineas.length()").value(1))
				.andExpect(jsonPath("$.empleadoId").exists());

		mvc.perform(get("/api/v1/ventas").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void una_venta_sin_lineas_devuelve_400() throws Exception {
		UUID[] ctx = montar();

		mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + ctx[0] + "\",\"lineas\":[]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/ventas")).andExpect(status().isUnauthorized());
	}
}
