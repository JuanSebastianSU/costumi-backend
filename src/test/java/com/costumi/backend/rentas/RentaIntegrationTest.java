package com.costumi.backend.rentas;

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

/** Rentas (RF-3): crear con importe, ciclo de estados y aislamiento por tenant. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RentaIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private record Ctx(String dueno, UUID sucursal, UUID cliente, UUID prenda) {
	}

	private UUID postId(String path, String token, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	private Ctx montar() throws Exception {
		return montar(2);
	}

	private Ctx montar(int stock) throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Renta " + UUID.randomUUID() + "\"}"))
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
				+ "\",\"nombre\":\"Camisa\",\"tipoArticulo\":\"RENTA\",\"precioRenta\":20.00}");
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno,
				"{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":" + stock + "}");
		return new Ctx(dueno, sucursal, cliente, prenda);
	}

	private String rentaBody(Ctx c, String retiro, String devolucion) {
		return "{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente() + "\",\"prendaId\":\""
				+ c.prenda() + "\",\"fechaRetiro\":\"" + retiro + "\",\"fechaDevolucion\":\"" + devolucion
				+ "\",\"precioPorDia\":20.00,\"deposito\":50.00}";
	}

	private UUID crearRenta(Ctx c) throws Exception {
		return postId("/api/v1/rentas", c.dueno(), rentaBody(c, "2026-08-01", "2026-08-04"));
	}

	@Test
	void crear_renta_en_sucursal_inexistente_devuelve_400() throws Exception {
		Ctx c = montar();
		// SEC-1: la renta no puede anclarse a una sucursal inexistente/ajena/archivada.
		String body = "{\"sucursalId\":\"" + UUID.randomUUID() + "\",\"clienteId\":\"" + c.cliente() + "\",\"prendaId\":\""
				+ c.prenda() + "\",\"fechaRetiro\":\"2026-08-01\",\"fechaDevolucion\":\"2026-08-04\","
				+ "\"precioPorDia\":20.00,\"deposito\":50.00}";
		mvc.perform(post("/api/v1/rentas").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void crear_renta_con_cliente_inexistente_devuelve_400() throws Exception {
		Ctx c = montar();
		// SEC-2: la renta no puede anclarse a un cliente inexistente/ajeno.
		String body = "{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + UUID.randomUUID() + "\",\"prendaId\":\""
				+ c.prenda() + "\",\"fechaRetiro\":\"2026-08-01\",\"fechaDevolucion\":\"2026-08-04\","
				+ "\"precioPorDia\":20.00,\"deposito\":50.00}";
		mvc.perform(post("/api/v1/rentas").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void un_cliente_en_lista_negra_no_puede_rentar_409() throws Exception {
		Ctx c = montar();
		// B1 (RF-7.4): poner al cliente en lista negra y luego intentar rentar -> 409.
		mvc.perform(post("/api/v1/clientes/{id}/lista-negra", c.cliente()).header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON).content("{\"enListaNegra\":true}"))
				.andExpect(status().isOk());

		mvc.perform(post("/api/v1/rentas").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON).content(rentaBody(c, "2026-08-01", "2026-08-04")))
				.andExpect(status().isConflict());
	}

	@Test
	void extender_una_renta_recalcula_el_importe() throws Exception {
		Ctx c = montar();
		UUID renta = crearRenta(c);
		mvc.perform(post("/api/v1/rentas/{id}/entregar", renta).header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk());

		// 3 días (importe 60) -> extender a 2026-08-08 = 7 días (importe 140), RF-3.6.
		mvc.perform(post("/api/v1/rentas/{id}/extender", renta).header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON).content("{\"nuevaFechaDevolucion\":\"2026-08-08\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fechaDevolucion").value("2026-08-08"))
				.andExpect(jsonPath("$.importe").value(140.00));

		// Extender a una fecha que no es posterior a la actual -> 400.
		mvc.perform(post("/api/v1/rentas/{id}/extender", renta).header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON).content("{\"nuevaFechaDevolucion\":\"2026-08-05\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void la_renta_registra_el_empleado_que_la_creo() throws Exception {
		Ctx c = montar();

		// RF-1.4: la renta queda asociada al usuario logueado que la creó.
		mvc.perform(post("/api/v1/rentas").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON).content(rentaBody(c, "2026-08-01", "2026-08-04")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.empleadoId").exists());
	}

	@Test
	void crear_calcula_importe_y_recorre_el_ciclo() throws Exception {
		Ctx c = montar();
		UUID renta = crearRenta(c);

		mvc.perform(post("/api/v1/rentas/{id}/entregar", renta).header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("ACTIVA"))
				.andExpect(jsonPath("$.importe").value(60.00));

		mvc.perform(post("/api/v1/rentas/{id}/devolver", renta).header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("DEVUELTA"));

		mvc.perform(get("/api/v1/rentas").param("clienteId", c.cliente().toString())
						.header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == '" + renta + "')]").exists());
	}

	@Test
	void devolver_una_reservada_devuelve_409() throws Exception {
		Ctx c = montar();
		UUID renta = crearRenta(c);

		mvc.perform(post("/api/v1/rentas/{id}/devolver", renta).header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isConflict());
	}

	@Test
	void sin_disponibilidad_en_fechas_traslapadas_devuelve_409() throws Exception {
		Ctx c = montar(1); // una sola unidad
		crearRenta(c); // 08-01..08-04 ocupa la única unidad

		// Segunda renta que se traslapa -> no hay disponibilidad -> 409.
		mvc.perform(post("/api/v1/rentas").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON).content(rentaBody(c, "2026-08-03", "2026-08-06")))
				.andExpect(status().isConflict());
	}

	/** Crea una segunda prenda de renta con stock en la sucursal del contexto y la devuelve. */
	private UUID crearPrendaConStock(Ctx c, int stock) throws Exception {
		UUID categoria = postId("/api/v1/categorias", c.dueno(), "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", c.dueno(), "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Extra\",\"tipoArticulo\":\"RENTA\",\"precioRenta\":30.00}");
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", c.dueno(),
				"{\"sucursalId\":\"" + c.sucursal() + "\",\"combinacion\":[],\"cantidadInicial\":" + stock + "}");
		return prenda;
	}

	@Test
	void crear_renta_multi_articulo_suma_el_importe_de_las_lineas() throws Exception {
		Ctx c = montar(3); // prenda principal con 3 unidades
		UUID otra = crearPrendaConStock(c, 2); // segunda prenda con 2 unidades

		// 3 días (08-01..08-04). Línea1: prenda x2 @20 = 40/día; Línea2: otra x1 @30 = 30/día. Total 70/día -> 210.
		String body = "{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente() + "\","
				+ "\"fechaRetiro\":\"2026-08-01\",\"fechaDevolucion\":\"2026-08-04\",\"deposito\":50.00,\"lineas\":["
				+ "{\"prendaId\":\"" + c.prenda() + "\",\"cantidad\":2,\"precioPorDia\":20.00},"
				+ "{\"prendaId\":\"" + otra + "\",\"cantidad\":1,\"precioPorDia\":30.00}]}";

		mvc.perform(post("/api/v1/rentas").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.importe").value(210.00))
				.andExpect(jsonPath("$.lineas.length()").value(2))
				.andExpect(jsonPath("$.prendaId").value(c.prenda().toString())); // artículo principal = 1ª línea
	}

	@Test
	void multi_articulo_sin_stock_de_una_linea_devuelve_409() throws Exception {
		Ctx c = montar(3);
		UUID otra = crearPrendaConStock(c, 1); // solo 1 unidad de la segunda prenda

		// Se piden 2 de la segunda prenda pero solo hay 1 -> 409 (RF-3.2 por línea).
		String body = "{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente() + "\","
				+ "\"fechaRetiro\":\"2026-08-01\",\"fechaDevolucion\":\"2026-08-04\",\"lineas\":["
				+ "{\"prendaId\":\"" + otra + "\",\"cantidad\":2,\"precioPorDia\":30.00}]}";

		mvc.perform(post("/api/v1/rentas").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict());
	}

	@Test
	void se_puede_rentar_en_fechas_que_no_se_traslapan() throws Exception {
		Ctx c = montar(1);
		crearRenta(c); // 08-01..08-04

		// Fechas disjuntas -> disponible -> 201.
		mvc.perform(post("/api/v1/rentas").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON).content(rentaBody(c, "2026-08-10", "2026-08-12")))
				.andExpect(status().isCreated());
	}

	@Test
	void rentar_una_prenda_inexistente_devuelve_400() throws Exception {
		Ctx c = montar(1);
		String body = "{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente()
				+ "\",\"prendaId\":\"" + UUID.randomUUID() + "\",\"fechaRetiro\":\"2026-08-01\","
				+ "\"fechaDevolucion\":\"2026-08-04\",\"precioPorDia\":20.00,\"deposito\":50.00}";
		mvc.perform(post("/api/v1/rentas").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void la_clave_de_idempotencia_no_duplica_la_renta() throws Exception {
		Ctx c = montar(1);
		String body = "{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente() + "\",\"prendaId\":\""
				+ c.prenda() + "\",\"fechaRetiro\":\"2026-09-01\",\"fechaDevolucion\":\"2026-09-03\","
				+ "\"precioPorDia\":20.00,\"deposito\":0,\"claveIdempotencia\":\"K-" + UUID.randomUUID() + "\"}";

		mvc.perform(post("/api/v1/rentas").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
		mvc.perform(post("/api/v1/rentas").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());

		mvc.perform(get("/api/v1/rentas").param("clienteId", c.cliente().toString())
						.header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1)); // no se duplicó (RF-17.6)
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/rentas")).andExpect(status().isUnauthorized());
	}
}
