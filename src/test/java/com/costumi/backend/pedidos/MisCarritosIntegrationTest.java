package com.costumi.backend.pedidos;

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
 * Los carritos abiertos del cliente en TODAS las tiendas (RF-16.2/16.3).
 *
 * <p>Antes no había forma de volver a un carrito: consultarlo exige saber de antemano empresa, sucursal
 * y tipo, y el cliente no tenía dónde verlos — al cambiar de tienda había que agregar un artículo otra
 * vez para que reapareciera.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MisCarritosIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private record Tienda(UUID empresa, String dueno, UUID sucursal, UUID prenda) {
	}

	@Test
	void el_cliente_ve_sus_carritos_de_todas_las_tiendas() throws Exception {
		Tienda a = montarTienda();
		Tienda b = montarTienda();
		String cliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);

		agregarComoCliente(cliente, a, "VENTA", 2);
		agregarComoCliente(cliente, b, "RENTA", 1);

		mvc.perform(get("/api/v1/carritos/mios").header("Authorization", "Bearer " + cliente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				// Trae los tres datos que hacen falta para volver al carrito, y el nombre para pintarlo.
				.andExpect(jsonPath("$[?(@.tipo == 'VENTA')].empresaId").value(a.empresa().toString()))
				.andExpect(jsonPath("$[?(@.tipo == 'VENTA')].articulos").value(2))
				.andExpect(jsonPath("$[?(@.tipo == 'RENTA')].empresaId").value(b.empresa().toString()))
				.andExpect(jsonPath("$[?(@.tipo == 'RENTA')].articulos").value(1))
				.andExpect(jsonPath("$[0].empresaNombre").isNotEmpty())
				.andExpect(jsonPath("$[0].sucursalNombre").isNotEmpty());
	}

	/** Renta y venta son carritos distintos en la MISMA tienda (RF-16.3): tienen que verse los dos. */
	@Test
	void renta_y_venta_de_la_misma_tienda_son_dos_carritos() throws Exception {
		Tienda t = montarTienda();
		String cliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);

		agregarComoCliente(cliente, t, "VENTA", 1);
		agregarComoCliente(cliente, t, "RENTA", 1);

		mvc.perform(get("/api/v1/carritos/mios").header("Authorization", "Bearer " + cliente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	/** Cada uno ve lo suyo: se resuelve por el usuario del token, no por un id del request. */
	@Test
	void un_cliente_no_ve_los_carritos_de_otro() throws Exception {
		Tienda t = montarTienda();
		String unCliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);
		String otroCliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);

		agregarComoCliente(unCliente, t, "VENTA", 1);

		mvc.perform(get("/api/v1/carritos/mios").header("Authorization", "Bearer " + otroCliente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	/**
	 * Solo carritos CON líneas: mirar una tienda ya resuelve/crea el carrito vacío, y listarlos llenaría
	 * la pantalla de tiendas que el cliente solo visitó.
	 */
	@Test
	void un_carrito_vacio_no_aparece() throws Exception {
		Tienda t = montarTienda();
		String cliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);

		// Un GET del carrito basta para crear la ficha y el carrito vacio de esa tienda.
		mvc.perform(get("/api/v1/carritos").header("Authorization", "Bearer " + cliente)
				.param("empresaId", t.empresa().toString())
				.param("sucursalId", t.sucursal().toString())
				.param("tipo", "VENTA"));

		mvc.perform(get("/api/v1/carritos/mios").header("Authorization", "Bearer " + cliente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void sin_token_no_se_pueden_consultar() throws Exception {
		mvc.perform(get("/api/v1/carritos/mios")).andExpect(status().isUnauthorized());
	}

	private void agregarComoCliente(String tokenCliente, Tienda t, String tipo, int cantidad) throws Exception {
		String fechas = "RENTA".equals(tipo)
				? ",\"fechaRetiro\":\"2026-08-01\",\"fechaDevolucion\":\"2026-08-04\""
				: "";
		mvc.perform(post("/api/v1/carritos/items").header("Authorization", "Bearer " + tokenCliente)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"empresaId\":\"" + t.empresa() + "\",\"sucursalId\":\"" + t.sucursal()
								+ "\",\"tipo\":\"" + tipo + "\",\"prendaId\":\"" + t.prenda()
								+ "\",\"cantidad\":" + cantidad + fechas + "}"))
				.andExpect(status().isOk());
	}

	private Tienda montarTienda() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Tienda " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());

		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());

		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"Centro\"}");
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Camisa\",\"tipoArticulo\":\"AMBOS\",\"precioRenta\":30.00,\"precioVenta\":90.00}");
		mvc.perform(post("/api/v1/prendas/{id}/grupos-stock", prenda).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"cantidad\":10}"))
				.andExpect(status().isCreated());
		return new Tienda(empresa, dueno, sucursal, prenda);
	}

	private UUID postId(String path, String token, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}
}
