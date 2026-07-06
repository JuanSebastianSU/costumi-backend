package com.costumi.backend.devoluciones;

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

/** Devoluciones (RF-5): checklist + liquidación del depósito. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DevolucionIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private UUID postId(String path, String token, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	private UUID rentaDePrueba() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Dev " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		this.dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"Centro\"}");
		UUID cliente = postId("/api/v1/clientes", dueno, "{\"nombre\":\"Cliente\"}");
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Camisa " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Camisa\",\"tipoArticulo\":\"RENTA\",\"precioRenta\":20.00}");
		this.prenda = prenda;
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno, "{\"combinacion\":[],\"cantidadInicial\":1}");
		return postId("/api/v1/rentas", dueno, "{\"sucursalId\":\"" + sucursal + "\",\"clienteId\":\"" + cliente
				+ "\",\"prendaId\":\"" + prenda + "\",\"fechaRetiro\":\"2026-08-01\",\"fechaDevolucion\":\"2026-08-04\","
				+ "\"precioPorDia\":20.00,\"deposito\":100.00}");
	}

	private String dueno;
	private UUID prenda;

	@Test
	void registrar_devolucion_liquida_deposito_guarda_checklist_y_actualiza_inventario() throws Exception {
		UUID renta = rentaDePrueba();

		mvc.perform(post("/api/v1/devoluciones").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rentaId\":\"" + renta + "\",\"deposito\":100.00,\"cargoPorDanos\":30.00,"
								+ "\"cargoPorRetraso\":10.00,\"piezas\":[{\"descripcion\":\"Camisa\",\"llego\":true,"
								+ "\"estado\":\"DANADA\"}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.remanente").value(60.00))
				.andExpect(jsonPath("$.piezas.length()").value(1))
				.andExpect(jsonPath("$.piezas[0].estado").value("DANADA"));

		mvc.perform(get("/api/v1/devoluciones").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.rentaId == '" + renta + "')]").exists());

		// RF-5.4/5.6: la pieza dañada movió la unidad de disponible a dañada.
		mvc.perform(get("/api/v1/prendas/{id}/grupos-stock", prenda).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].disponibles").value(0))
				.andExpect(jsonPath("$[0].danadas").value(1));
	}

	@Test
	void devolver_una_renta_de_otra_empresa_devuelve_400() throws Exception {
		rentaDePrueba(); // deja this.dueno de la empresa A con una renta
		String duenoDeA = this.dueno;

		mvc.perform(post("/api/v1/devoluciones").header("Authorization", "Bearer " + duenoDeA)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rentaId\":\"" + UUID.randomUUID() + "\",\"deposito\":100.00,\"cargoPorDanos\":0,"
								+ "\"cargoPorRetraso\":0,\"piezas\":[{\"descripcion\":\"X\",\"llego\":true,\"estado\":\"BIEN\"}]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/devoluciones")).andExpect(status().isUnauthorized());
	}
}
