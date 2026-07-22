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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Carrito (RF-16): agrega ítems, suma, y renta/venta quedan en carritos separados. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CarritoIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private record Ctx(UUID empresa, String dueno, UUID sucursal, UUID cliente, UUID prenda) {
	}

	private UUID postId(String path, String token, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	private Ctx montar() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Carrito " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());

		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());

		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"Centro\"}");
		UUID cliente = postId("/api/v1/clientes", dueno, "{\"nombre\":\"Cliente\"}");
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Camisa " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Camisa\",\"tipoArticulo\":\"AMBOS\",\"precioRenta\":30.00,\"precioVenta\":90.00}");
		return new Ctx(empresa, dueno, sucursal, cliente, prenda);
	}

	private void agregar(Ctx c, String tipo, int cantidad) throws Exception {
		// En RENTA cada artículo lleva su periodo (RF-18.6); aquí se usa uno fijo para todo el helper.
		String fechas = "RENTA".equals(tipo)
				? ",\"fechaRetiro\":\"2026-08-01\",\"fechaDevolucion\":\"2026-08-04\""
				: "";
		mvc.perform(post("/api/v1/carritos/items").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente()
								+ "\",\"tipo\":\"" + tipo + "\",\"prendaId\":\"" + c.prenda() + "\",\"cantidad\":" + cantidad
								+ fechas + "}"))
				.andExpect(status().isOk());
	}

	@Test
	void agregar_la_misma_prenda_suma_la_cantidad() throws Exception {
		Ctx c = montar();

		agregar(c, "RENTA", 2);
		agregar(c, "RENTA", 3);

		mvc.perform(get("/api/v1/carritos").header("Authorization", "Bearer " + c.dueno())
						.param("sucursalId", c.sucursal().toString())
						.param("clienteId", c.cliente().toString())
						.param("tipo", "RENTA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lineas.length()").value(1))
				.andExpect(jsonPath("$.lineas[0].cantidad").value(5));
	}

	@Test
	void renta_y_venta_quedan_en_carritos_separados() throws Exception {
		Ctx c = montar();

		agregar(c, "RENTA", 1);
		agregar(c, "VENTA", 1);

		mvc.perform(get("/api/v1/carritos").header("Authorization", "Bearer " + c.dueno())
						.param("sucursalId", c.sucursal().toString())
						.param("clienteId", c.cliente().toString())
						.param("tipo", "RENTA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("RENTA"));

		mvc.perform(get("/api/v1/carritos").header("Authorization", "Bearer " + c.dueno())
						.param("sucursalId", c.sucursal().toString())
						.param("clienteId", c.cliente().toString())
						.param("tipo", "VENTA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("VENTA"));
	}

	@Test
	void checkout_de_venta_crea_la_venta_y_confirma_el_carrito() throws Exception {
		Ctx c = montar();
		mvc.perform(post("/api/v1/prendas/{id}/grupos-stock", c.prenda()).header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"combinacion\":[],\"cantidadInicial\":5}"))
				.andExpect(status().isCreated());
		agregar(c, "VENTA", 2);

		String res = mvc.perform(post("/api/v1/carritos/checkout").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente() + "\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		org.assertj.core.api.Assertions.assertThat(json.readTree(res).get("ventaId").asText()).isNotBlank();

		// La venta quedó registrada (2 × 90 = 180).
		mvc.perform(get("/api/v1/ventas").header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.total == 180.00)]").exists());

		// RF-17.6: un segundo checkout del mismo carrito (ya confirmado) no crea otra venta -> 404.
		mvc.perform(post("/api/v1/carritos/checkout").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente() + "\"}"))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/ventas").header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(1)); // sigue habiendo una sola venta
	}

	@Test
	void checkout_de_renta_crea_una_renta_por_periodo_y_confirma_el_carrito() throws Exception {
		Ctx c = montar();
		mvc.perform(post("/api/v1/prendas/{id}/grupos-stock", c.prenda()).header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"combinacion\":[],\"cantidadInicial\":5}"))
				.andExpect(status().isCreated());

		// Dos artículos: uno con periodo A (x2) y otro con periodo B (x1) -> dos rentas.
		agregarRenta(c, 2, "2026-08-01", "2026-08-04");
		agregarRenta(c, 1, "2026-09-01", "2026-09-03");

		String res = mvc.perform(post("/api/v1/carritos/checkout-renta").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente() + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rentaIds.length()").value(2))
				.andReturn().getResponse().getContentAsString();
		org.assertj.core.api.Assertions.assertThat(json.readTree(res).get("rentaIds").get(0).asText()).isNotBlank();

		// Se crearon dos rentas para el cliente.
		mvc.perform(get("/api/v1/rentas").param("clienteId", c.cliente().toString())
						.header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(2));

		// El carrito de renta quedó confirmado (ya no hay pendiente) -> 404 al pedirlo.
		mvc.perform(get("/api/v1/carritos").header("Authorization", "Bearer " + c.dueno())
						.param("sucursalId", c.sucursal().toString())
						.param("clienteId", c.cliente().toString())
						.param("tipo", "RENTA"))
				.andExpect(status().isNotFound());
	}

	private void agregarRenta(Ctx c, int cantidad, String retiro, String devolucion) throws Exception {
		mvc.perform(post("/api/v1/carritos/items").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente()
								+ "\",\"tipo\":\"RENTA\",\"prendaId\":\"" + c.prenda() + "\",\"cantidad\":" + cantidad
								+ ",\"fechaRetiro\":\"" + retiro + "\",\"fechaDevolucion\":\"" + devolucion + "\"}"))
				.andExpect(status().isOk());
	}

	private void stock(Ctx c, int cantidad) throws Exception {
		mvc.perform(post("/api/v1/prendas/{id}/grupos-stock", c.prenda()).header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"combinacion\":[],\"cantidadInicial\":" + cantidad
								+ "}"))
				.andExpect(status().isCreated());
	}

	/** Disfraz con un único slot FIJO = la prenda del contexto (no requiere selección). */
	private UUID crearDisfrazFijo(Ctx c) throws Exception {
		String body = mvc.perform(post("/api/v1/disfraces").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Disfraz Fijo\",\"slots\":[{\"orden\":1,\"nombre\":\"Cuerpo\","
								+ "\"ejePrenda\":\"FIJA\",\"prendaFijaId\":\"" + c.prenda() + "\",\"opcional\":false}]}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	/** Disfraz con un slot PERSONALIZABLE cuya única opción explícita es la prenda del contexto. */
	private UUID crearDisfrazPersonalizable(Ctx c) throws Exception {
		String body = mvc.perform(post("/api/v1/disfraces").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Disfraz Pers\",\"slots\":[{\"orden\":1,\"nombre\":\"Cuerpo\","
								+ "\"ejePrenda\":\"PERSONALIZABLE\",\"prendasOpcion\":[\"" + c.prenda()
								+ "\"],\"opcional\":false}]}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	@Test
	void agregar_un_disfraz_de_venta_lo_valoriza_y_el_checkout_crea_la_venta() throws Exception {
		Ctx c = montar(); // prenda: precioVenta 90.00
		stock(c, 5);
		UUID disfraz = crearDisfrazFijo(c);

		// Agrega el disfraz (x2) al carrito de VENTA; su slot es FIJO, no necesita selección.
		mvc.perform(post("/api/v1/carritos/items").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente()
								+ "\",\"tipo\":\"VENTA\",\"disfrazId\":\"" + disfraz + "\",\"cantidad\":2}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lineas[0].disfrazId").value(disfraz.toString()))
				.andExpect(jsonPath("$.lineas[0].nombre").value("Disfraz Fijo"));

		// El total = 2 × (precio de venta de la pieza 90) = 180.
		mvc.perform(get("/api/v1/carritos").header("Authorization", "Bearer " + c.dueno())
						.param("sucursalId", c.sucursal().toString())
						.param("clienteId", c.cliente().toString())
						.param("tipo", "VENTA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lineas.length()").value(1))
				.andExpect(jsonPath("$.lineas[0].disfrazId").value(disfraz.toString()))
				.andExpect(jsonPath("$.lineas[0].precioUnitario").value(90.00))
				.andExpect(jsonPath("$.total").value(180.00));

		// Checkout: crea la venta y confirma el carrito.
		mvc.perform(post("/api/v1/carritos/checkout").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente() + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ventaId").exists());
		mvc.perform(get("/api/v1/ventas").header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.total == 180.00)]").exists());
	}

	@Test
	void agregar_un_disfraz_de_renta_con_seleccion_suma_cantidad_y_hace_checkout() throws Exception {
		Ctx c = montar(); // prenda: precioRenta 30.00 (por día)
		stock(c, 5);
		UUID disfraz = crearDisfrazPersonalizable(c);
		String seleccion = ",\"selecciones\":[{\"orden\":1,\"prendaId\":\"" + c.prenda() + "\"}]";
		String periodo = ",\"fechaRetiro\":\"2026-08-01\",\"fechaDevolucion\":\"2026-08-04\""; // 3 días

		// Misma selección + mismo periodo dos veces → suma a 3 (una sola línea).
		agregarDisfrazRenta(c, disfraz, 1, seleccion, periodo);
		agregarDisfrazRenta(c, disfraz, 2, seleccion, periodo);

		// total = 30 (por día) × 3 (cantidad) × 3 (días) = 270.
		mvc.perform(get("/api/v1/carritos").header("Authorization", "Bearer " + c.dueno())
						.param("sucursalId", c.sucursal().toString())
						.param("clienteId", c.cliente().toString())
						.param("tipo", "RENTA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lineas.length()").value(1))
				.andExpect(jsonPath("$.lineas[0].cantidad").value(3))
				.andExpect(jsonPath("$.lineas[0].selecciones.length()").value(1))
				.andExpect(jsonPath("$.lineas[0].selecciones[0].prendaId").value(c.prenda().toString()))
				.andExpect(jsonPath("$.total").value(270.00));

		// Checkout de renta: una renta (un solo periodo) y el carrito queda confirmado.
		mvc.perform(post("/api/v1/carritos/checkout-renta").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente() + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rentaIds.length()").value(1));
	}

	private void agregarDisfrazRenta(Ctx c, UUID disfraz, int cantidad, String seleccion, String periodo)
			throws Exception {
		mvc.perform(post("/api/v1/carritos/items").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente()
								+ "\",\"tipo\":\"RENTA\",\"disfrazId\":\"" + disfraz + "\",\"cantidad\":" + cantidad
								+ seleccion + periodo + "}"))
				.andExpect(status().isOk());
	}

	@Test
	void el_cliente_puede_quitar_un_item_del_carrito() throws Exception {
		Ctx c = montar();
		stock(c, 5);
		UUID disfraz = crearDisfrazFijo(c);
		agregar(c, "VENTA", 2);
		mvc.perform(post("/api/v1/carritos/items").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente()
								+ "\",\"tipo\":\"VENTA\",\"disfrazId\":\"" + disfraz + "\",\"cantidad\":1}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lineas.length()").value(2));

		// El id de la línea del disfraz es lo que se manda para quitarla.
		String carrito = mvc.perform(get("/api/v1/carritos").header("Authorization", "Bearer " + c.dueno())
						.param("sucursalId", c.sucursal().toString())
						.param("clienteId", c.cliente().toString())
						.param("tipo", "VENTA"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		String lineaDelDisfraz = null;
		for (com.fasterxml.jackson.databind.JsonNode linea : json.readTree(carrito).get("lineas")) {
			if (!linea.get("disfrazId").isNull()) {
				lineaDelDisfraz = linea.get("id").asText();
			}
		}
		org.assertj.core.api.Assertions.assertThat(lineaDelDisfraz).isNotNull();

		mvc.perform(delete("/api/v1/carritos/items/{lineaId}", lineaDelDisfraz)
						.header("Authorization", "Bearer " + c.dueno())
						.param("sucursalId", c.sucursal().toString())
						.param("clienteId", c.cliente().toString())
						.param("tipo", "VENTA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lineas.length()").value(1))
				.andExpect(jsonPath("$.lineas[0].prendaId").value(c.prenda().toString()));

		// Quitar algo que ya no está es un 400, no un 500.
		mvc.perform(delete("/api/v1/carritos/items/{lineaId}", lineaDelDisfraz)
						.header("Authorization", "Bearer " + c.dueno())
						.param("sucursalId", c.sucursal().toString())
						.param("clienteId", c.cliente().toString())
						.param("tipo", "VENTA"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void un_item_que_ya_no_se_puede_valorizar_no_bloquea_el_carrito_y_se_puede_quitar() throws Exception {
		Ctx c = montar();
		stock(c, 5);
		UUID disfraz = crearDisfrazFijo(c); // nace AMBOS: se puede comprar
		mvc.perform(post("/api/v1/carritos/items").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente()
								+ "\",\"tipo\":\"VENTA\",\"disfrazId\":\"" + disfraz + "\",\"cantidad\":1}"))
				.andExpect(status().isOk());

		// El dueño lo pasa a SOLO RENTA DESPUÉS de que el cliente ya lo tenía en el carrito de venta.
		mvc.perform(put("/api/v1/disfraces/{id}", disfraz).header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Disfraz Fijo\",\"tipo\":\"RENTA\",\"slots\":[{\"orden\":1,"
								+ "\"nombre\":\"Cuerpo\",\"ejePrenda\":\"FIJA\",\"prendaFijaId\":\"" + c.prenda()
								+ "\",\"opcional\":false}]}"))
				.andExpect(status().isOk());

		// El carrito NO se cae: la línea vuelve sin precio y con el motivo, para poder quitarla.
		String carrito = mvc.perform(get("/api/v1/carritos").header("Authorization", "Bearer " + c.dueno())
						.param("sucursalId", c.sucursal().toString())
						.param("clienteId", c.cliente().toString())
						.param("tipo", "VENTA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lineas[0].precioUnitario").doesNotExist())
				.andExpect(jsonPath("$.lineas[0].motivoNoDisponible").exists())
				.andReturn().getResponse().getContentAsString();

		String lineaId = json.readTree(carrito).get("lineas").get(0).get("id").asText();
		mvc.perform(delete("/api/v1/carritos/items/{lineaId}", lineaId)
						.header("Authorization", "Bearer " + c.dueno())
						.param("sucursalId", c.sucursal().toString())
						.param("clienteId", c.cliente().toString())
						.param("tipo", "VENTA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lineas.length()").value(0));
	}

	@Test
	void no_se_puede_agregar_prenda_y_disfraz_a_la_vez() throws Exception {
		Ctx c = montar();
		mvc.perform(post("/api/v1/carritos/items").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente()
								+ "\",\"tipo\":\"VENTA\",\"prendaId\":\"" + c.prenda() + "\",\"disfrazId\":\""
								+ UUID.randomUUID() + "\",\"cantidad\":1}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void cliente_del_marketplace_arma_su_carrito_y_hace_checkout() throws Exception {
		Ctx c = montar();
		// Stock para que el checkout de venta descuente inventario (RF-4.4).
		mvc.perform(post("/api/v1/prendas/{id}/grupos-stock", c.prenda()).header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"combinacion\":[],\"cantidadInicial\":5}"))
				.andExpect(status().isCreated());

		// Usuario CLIENTE del marketplace (sin empresa): compra para sí mismo (RF-18.5).
		String cliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);

		// Agrega al carrito de la tienda: manda empresaId (la tienda) y NO clienteId (usa su propia ficha).
		mvc.perform(post("/api/v1/carritos/items").header("Authorization", "Bearer " + cliente)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"empresaId\":\"" + c.empresa()
								+ "\",\"tipo\":\"VENTA\",\"prendaId\":\"" + c.prenda() + "\",\"cantidad\":1}"))
				.andExpect(status().isOk());

		// Checkout del propio cliente → crea la venta a su nombre.
		mvc.perform(post("/api/v1/carritos/checkout").header("Authorization", "Bearer " + cliente)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"empresaId\":\"" + c.empresa() + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ventaId").exists());

		// "Mis Pedidos": el cliente ve su compra en su historial, en todas las tiendas (RF-18.9).
		mvc.perform(get("/api/v1/clientes/me/historial").header("Authorization", "Bearer " + cliente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void el_disfraz_no_se_pierde_al_cobrar_y_el_dueno_puede_rankearlo() throws Exception {
		// Antes, al confirmar la compra el disfraz se resolvia a sus prendas y desaparecia: el cliente
		// veia las PIEZAS en su pedido y el dueño no podia saber que DISFRAZ se vende mas.
		Ctx c = montar();
		stock(c, 5);
		UUID disfraz = crearDisfrazFijo(c);
		mvc.perform(post("/api/v1/carritos/items").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente()
								+ "\",\"tipo\":\"VENTA\",\"disfrazId\":\"" + disfraz + "\",\"cantidad\":2}"))
				.andExpect(status().isOk());
		mvc.perform(post("/api/v1/carritos/checkout").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente() + "\"}"))
				.andExpect(status().isOk());

		// La venta recuerda de que disfraz salio cada linea, con su nombre.
		mvc.perform(get("/api/v1/ventas").header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[0].lineas[0].disfrazId").value(disfraz.toString()))
				.andExpect(jsonPath("$.contenido[0].lineas[0].disfrazNombre").value("Disfraz Fijo"))
				.andExpect(jsonPath("$.contenido[0].lineas[0].disfrazGrupo").exists())
				.andExpect(jsonPath("$.contenido[0].lineas[0].disfrazCantidad").value(2));

		// Y "Mis pedidos" del cliente dice QUE compro, no las piezas sueltas.
		mvc.perform(get("/api/v1/clientes/{id}/historial", c.cliente())
						.header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].lineas[0].disfrazNombre").value("Disfraz Fijo"))
				.andExpect(jsonPath("$[0].lineas[0].disfrazGrupo").exists());

		// Y el dueño puede ver que DISFRAZ se vende mas: cuenta disfraces (2), no piezas.
		mvc.perform(get("/api/v1/reportes/disfraces-mas-vendidos").header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].disfrazId").value(disfraz.toString()))
				.andExpect(jsonPath("$[0].nombre").value("Disfraz Fijo"))
				.andExpect(jsonPath("$[0].unidades").value(2));
	}

	@Test
	void una_prenda_suelta_no_queda_marcada_como_disfraz() throws Exception {
		Ctx c = montar();
		stock(c, 5);
		agregar(c, "VENTA", 1);
		mvc.perform(post("/api/v1/carritos/checkout").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente() + "\"}"))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v1/ventas").header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[0].lineas[0].disfrazId").doesNotExist());
	}

	@Test
	void el_carrito_muestra_precio_por_linea_y_total() throws Exception {
		Ctx c = montar(); // prenda: precioRenta 30.00 (por día), precioVenta 90.00

		// VENTA x2 -> 90 × 2 = 180. El cliente ve el total ANTES de confirmar (RF-18.5).
		agregar(c, "VENTA", 2);
		String venta = mvc.perform(get("/api/v1/carritos").header("Authorization", "Bearer " + c.dueno())
						.param("sucursalId", c.sucursal().toString())
						.param("clienteId", c.cliente().toString())
						.param("tipo", "VENTA"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		org.assertj.core.api.Assertions.assertThat(json.readTree(venta).get("total").decimalValue())
				.isEqualByComparingTo("180.00");
		org.assertj.core.api.Assertions.assertThat(
				json.readTree(venta).get("lineas").get(0).get("precioUnitario").decimalValue())
				.isEqualByComparingTo("90.00");
		org.assertj.core.api.Assertions.assertThat(
				json.readTree(venta).get("lineas").get(0).get("subtotal").decimalValue())
				.isEqualByComparingTo("180.00");
		// La línea trae el nombre de la prenda para mostrar QUÉ se agregó (con imagen si tiene).
		org.assertj.core.api.Assertions.assertThat(
				json.readTree(venta).get("lineas").get(0).get("nombre").asText())
				.isEqualTo("Camisa");

		// RENTA x2, periodo 01→04 ago (3 días) -> 30 × 2 × 3 = 180.
		agregar(c, "RENTA", 2);
		String renta = mvc.perform(get("/api/v1/carritos").header("Authorization", "Bearer " + c.dueno())
						.param("sucursalId", c.sucursal().toString())
						.param("clienteId", c.cliente().toString())
						.param("tipo", "RENTA"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		org.assertj.core.api.Assertions.assertThat(json.readTree(renta).get("total").decimalValue())
				.isEqualByComparingTo("180.00");
		org.assertj.core.api.Assertions.assertThat(
				json.readTree(renta).get("lineas").get(0).get("subtotal").decimalValue())
				.isEqualByComparingTo("180.00");
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/carritos")
						.param("sucursalId", UUID.randomUUID().toString())
						.param("clienteId", UUID.randomUUID().toString())
						.param("tipo", "RENTA"))
				.andExpect(status().isUnauthorized());
	}
}
