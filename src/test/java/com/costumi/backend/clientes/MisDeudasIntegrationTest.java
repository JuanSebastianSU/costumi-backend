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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El cliente ve sus propias multas y saldos (RF-7/11.5).
 *
 * <p>El estado de cuenta que ya existía es por empresa y lo mira la tienda: el cliente no tenía forma de
 * ver qué le cobraron ni por qué.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MisDeudasIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void el_cliente_ve_su_multa_con_el_desglose_y_la_tienda() throws Exception {
		Escenario e = montarRentaConMulta();

		mvc.perform(get("/api/v1/clientes/me/deudas").header("Authorization", "Bearer " + e.cliente()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].empresaNombre").value(e.tiendaNombre()))
				// El desglose es lo que explica la multa: 150 de daños contra 50 de depósito -> multa 100.
				.andExpect(jsonPath("$[0].cargoPorDanos").value(150))
				.andExpect(jsonPath("$[0].deposito").value(50))
				.andExpect(jsonPath("$[0].multa").value(100))
				.andExpect(jsonPath("$[0].codigoRetiro").isNotEmpty());
	}

	/** Se resuelve por el usuario del token: nadie ve lo que debe otro. */
	@Test
	void un_cliente_no_ve_las_deudas_de_otro() throws Exception {
		montarRentaConMulta();
		String ajeno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);

		mvc.perform(get("/api/v1/clientes/me/deudas").header("Authorization", "Bearer " + ajeno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void sin_token_no_se_pueden_consultar() throws Exception {
		mvc.perform(get("/api/v1/clientes/me/deudas")).andExpect(status().isUnauthorized());
	}

	private record Escenario(String cliente, String tiendaNombre) {
	}

	/**
	 * Monta una tienda, un cliente del marketplace con ficha, y una renta suya devuelta con daños por
	 * encima del depósito (que es lo que produce multa).
	 */
	private Escenario montarRentaConMulta() throws Exception {
		String tiendaNombre = "Tienda " + UUID.randomUUID();
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + tiendaNombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());

		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());

		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"Centro\"}");
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Traje\",\"tipoArticulo\":\"RENTA\",\"precioRenta\":40.00}");
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno,
				"{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":5}");

		// El cliente del marketplace: su ficha se crea al tocar el carrito de esa tienda.
		String cliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);
		mvc.perform(get("/api/v1/carritos").header("Authorization", "Bearer " + cliente)
				.param("empresaId", empresa.toString())
				.param("sucursalId", sucursal.toString())
				.param("tipo", "RENTA"));
		UUID ficha = UUID.fromString(json.readTree(mvc.perform(get("/api/v1/clientes")
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
				.get("contenido").get(0).get("id").asText());

		// Renta de esa ficha, entregada y devuelta con daños por encima del deposito -> multa.
		UUID renta = postId("/api/v1/rentas", dueno, "{\"sucursalId\":\"" + sucursal + "\",\"clienteId\":\"" + ficha
				+ "\",\"prendaId\":\"" + prenda + "\",\"fechaRetiro\":\"2026-08-01\""
				+ ",\"fechaDevolucion\":\"2026-08-04\",\"precioPorDia\":20.00,\"deposito\":50.00}");
		mvc.perform(post("/api/v1/rentas/{id}/entregar", renta).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk());
		// Daños 150 contra un depósito de 50: la multa es lo que excede el depósito (100).
		mvc.perform(post("/api/v1/devoluciones").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rentaId\":\"" + renta + "\",\"deposito\":50.00,\"cargoPorDanos\":150.00,"
								+ "\"cargoPorRetraso\":0.00,\"piezas\":[{\"prendaId\":\"" + prenda
								+ "\",\"descripcion\":\"Traje\",\"llego\":true,\"estado\":\"DANADA\"}]}"))
				.andExpect(status().isCreated());

		return new Escenario(cliente, tiendaNombre);
	}

	private UUID postId(String path, String token, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}
}
