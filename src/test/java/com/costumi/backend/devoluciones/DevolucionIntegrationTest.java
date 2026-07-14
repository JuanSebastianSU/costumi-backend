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
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno,
				"{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":1}");
		UUID renta = postId("/api/v1/rentas", dueno, "{\"sucursalId\":\"" + sucursal + "\",\"clienteId\":\"" + cliente
				+ "\",\"prendaId\":\"" + prenda + "\",\"fechaRetiro\":\"2026-08-01\",\"fechaDevolucion\":\"2026-08-04\","
				+ "\"precioPorDia\":20.00,\"deposito\":100.00}");
		// La renta debe estar ACTIVA (entregada) para poder devolverse (RF-5.1).
		mvc.perform(post("/api/v1/rentas/{id}/entregar", renta).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk());
		return renta;
	}

	private String dueno;
	private UUID prenda;

	/** Igual que rentaDePrueba pero con fechas pasadas (pactada 2020-01-05) para probar el retraso. */
	private UUID rentaVencida() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"DevV " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		this.dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"Centro\"}");
		UUID cliente = postId("/api/v1/clientes", dueno, "{\"nombre\":\"Cliente\"}");
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Camisa " + UUID.randomUUID() + "\"}");
		UUID prendaId = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Camisa\",\"tipoArticulo\":\"RENTA\",\"precioRenta\":20.00}");
		this.prenda = prendaId;
		postId("/api/v1/prendas/" + prendaId + "/grupos-stock", dueno,
				"{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":1}");
		UUID renta = postId("/api/v1/rentas", dueno, "{\"sucursalId\":\"" + sucursal + "\",\"clienteId\":\"" + cliente
				+ "\",\"prendaId\":\"" + prendaId + "\",\"fechaRetiro\":\"2020-01-01\",\"fechaDevolucion\":\"2020-01-05\","
				+ "\"precioPorDia\":20.00,\"deposito\":100.00}");
		mvc.perform(post("/api/v1/rentas/{id}/entregar", renta).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk());
		return renta;
	}

	@Test
	void registrar_devolucion_liquida_deposito_guarda_checklist_y_actualiza_inventario() throws Exception {
		UUID renta = rentaDePrueba();

		mvc.perform(post("/api/v1/devoluciones").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rentaId\":\"" + renta + "\",\"deposito\":100.00,\"cargoPorDanos\":30.00,"
								+ "\"cargoPorRetraso\":10.00,\"piezas\":[{\"prendaId\":\"" + prenda + "\",\"descripcion\":\"Camisa\",\"llego\":true,"
								+ "\"estado\":\"DANADA\"}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.remanente").value(60.00))
				.andExpect(jsonPath("$.multa").value(0)) // cargos 40 < depósito 100 -> sin multa (RF-5.2)
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

		// RF-5.1: la renta quedó DEVUELTA (checklist conectado).
		mvc.perform(get("/api/v1/rentas").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + renta + "' && @.estado == 'DEVUELTA')]").exists());
	}

	@Test
	void una_devolucion_con_multa_dispara_una_notificacion_al_cliente() throws Exception {
		UUID renta = rentaDePrueba();

		// Cargos 60+20=80 > depósito 50 -> multa 30 -> se dispara la notificación (RF-11.1, §5.5).
		mvc.perform(post("/api/v1/devoluciones").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rentaId\":\"" + renta + "\",\"deposito\":50.00,\"cargoPorDanos\":60.00,"
								+ "\"cargoPorRetraso\":20.00,\"piezas\":[{\"prendaId\":\"" + prenda + "\",\"descripcion\":\"Camisa\",\"llego\":true,"
								+ "\"estado\":\"DANADA\"}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.multa").value(30.00));

		mvc.perform(get("/api/v1/notificaciones").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.canal == 'EMAIL')]").exists());
	}

	@Test
	void con_el_modulo_de_multas_apagado_no_se_notifica() throws Exception {
		UUID renta = rentaDePrueba();

		// Apagar el módulo de multas (RF-12.4/6.6).
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/configuracion")
						.header("Authorization", "Bearer " + dueno).contentType(MediaType.APPLICATION_JSON)
						.content("{\"conteoStock\":true,\"multasActivo\":false,\"multiSucursal\":false,\"pagoEnLinea\":false}"))
				.andExpect(status().isOk());

		mvc.perform(post("/api/v1/devoluciones").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rentaId\":\"" + renta + "\",\"deposito\":50.00,\"cargoPorDanos\":60.00,"
								+ "\"cargoPorRetraso\":20.00,\"piezas\":[{\"prendaId\":\"" + prenda + "\",\"descripcion\":\"Camisa\",\"llego\":true,"
								+ "\"estado\":\"DANADA\"}]}"))
				.andExpect(status().isCreated())
				// Multas OFF (RF-6.6): NO se genera ningún cargo -> ni daños ni retraso -> multa 0 y
				// remanente = depósito completo.
				.andExpect(jsonPath("$.cargoPorDanos").value(0))
				.andExpect(jsonPath("$.cargoPorRetraso").value(0))
				.andExpect(jsonPath("$.multa").value(0))
				.andExpect(jsonPath("$.remanente").value(50.00));

		// Con el switch apagado, no se generó notificación de multa.
		mvc.perform(get("/api/v1/notificaciones").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.canal == 'EMAIL')]").doesNotExist());
	}

	@Test
	void el_recargo_por_retraso_se_deriva_del_recargo_configurado_por_dia() throws Exception {
		// Renta vencida: pactada 2020-01-05, entregada de verdad 5 días tarde (2020-01-10).
		UUID renta = rentaVencida();

		// Configura el recargo por día en 10 (RF-12.2).
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/configuracion")
						.header("Authorization", "Bearer " + dueno).contentType(MediaType.APPLICATION_JSON)
						.content("{\"conteoStock\":true,\"multasActivo\":true,\"multiSucursal\":false,\"pagoEnLinea\":false,"
								+ "\"recargoPorRetrasoPorDia\":10.00}"))
				.andExpect(status().isOk());

		// Sin pasar cargoPorRetraso: se deriva = 10/día × 5 días = 50. Depósito 100, sin daños -> remanente 50.
		mvc.perform(post("/api/v1/devoluciones").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rentaId\":\"" + renta + "\",\"deposito\":100.00,\"cargoPorDanos\":0,"
								+ "\"fechaDevolucionReal\":\"2020-01-10\",\"piezas\":[{\"prendaId\":\"" + prenda
								+ "\",\"descripcion\":\"Camisa\",\"llego\":true,\"estado\":\"BIEN\"}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cargoPorRetraso").value(50.00))
				.andExpect(jsonPath("$.remanente").value(50.00));
	}

	@Test
	void con_politica_de_recargo_fija_el_retraso_es_un_monto_unico() throws Exception {
		// Misma renta vencida (5 días tarde), pero con política de recargo FIJA.
		UUID renta = rentaVencida();

		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/configuracion")
						.header("Authorization", "Bearer " + dueno).contentType(MediaType.APPLICATION_JSON)
						.content("{\"conteoStock\":true,\"multasActivo\":true,\"multiSucursal\":false,\"pagoEnLinea\":false,"
								+ "\"recargoPorRetrasoPorDia\":10.00,\"modoRecargoRetraso\":\"FIJA\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.modoRecargoRetraso").value("FIJA"));

		// Fija: 5 días de atraso -> se cobra el monto único 10 (no 10×5=50). Depósito 100 -> remanente 90.
		mvc.perform(post("/api/v1/devoluciones").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rentaId\":\"" + renta + "\",\"deposito\":100.00,\"cargoPorDanos\":0,"
								+ "\"fechaDevolucionReal\":\"2020-01-10\",\"piezas\":[{\"prendaId\":\"" + prenda
								+ "\",\"descripcion\":\"Camisa\",\"llego\":true,\"estado\":\"BIEN\"}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cargoPorRetraso").value(10.00))
				.andExpect(jsonPath("$.remanente").value(90.00));
	}

	@Test
	void devolver_una_renta_de_otra_empresa_devuelve_400() throws Exception {
		rentaDePrueba(); // deja this.dueno de la empresa A con una renta
		String duenoDeA = this.dueno;

		mvc.perform(post("/api/v1/devoluciones").header("Authorization", "Bearer " + duenoDeA)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rentaId\":\"" + UUID.randomUUID() + "\",\"deposito\":100.00,\"cargoPorDanos\":0,"
								+ "\"cargoPorRetraso\":0,\"piezas\":[{\"prendaId\":\"" + UUID.randomUUID()
						+ "\",\"descripcion\":\"X\",\"llego\":true,\"estado\":\"BIEN\"}]}"))
				.andExpect(status().isBadRequest());
	}

	/** Renta de 2 unidades de una prenda (línea con cantidad 2), entregada y lista para devolverse. */
	private UUID rentaDeDosUnidades() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"DevP " + UUID.randomUUID() + "\"}"))
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
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno,
				"{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":2}");
		UUID renta = postId("/api/v1/rentas", dueno, "{\"sucursalId\":\"" + sucursal + "\",\"clienteId\":\"" + cliente
				+ "\",\"fechaRetiro\":\"2026-08-01\",\"fechaDevolucion\":\"2026-08-04\",\"deposito\":100.00,"
				+ "\"lineas\":[{\"prendaId\":\"" + prenda + "\",\"cantidad\":2,\"precioPorDia\":20.00}]}");
		mvc.perform(post("/api/v1/rentas/{id}/entregar", renta).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk());
		return renta;
	}

	@Test
	void devolucion_parcial_deja_la_renta_activa_hasta_devolver_todo() throws Exception {
		UUID renta = rentaDeDosUnidades();

		// 1ª devolución: solo 1 de las 2 unidades (BIEN). La renta sigue ACTIVA (RF-5.5).
		mvc.perform(post("/api/v1/devoluciones").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rentaId\":\"" + renta + "\",\"deposito\":100.00,\"cargoPorDanos\":0,"
								+ "\"cargoPorRetraso\":0,\"piezas\":[{\"prendaId\":\"" + prenda
								+ "\",\"descripcion\":\"Unidad 1\",\"llego\":true,\"estado\":\"BIEN\"}]}"))
				.andExpect(status().isCreated());
		mvc.perform(get("/api/v1/rentas").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + renta + "' && @.estado == 'ACTIVA')]").exists());

		// 2ª devolución: la unidad restante. Ahora sí queda DEVUELTA.
		mvc.perform(post("/api/v1/devoluciones").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rentaId\":\"" + renta + "\",\"deposito\":100.00,\"cargoPorDanos\":0,"
								+ "\"cargoPorRetraso\":0,\"piezas\":[{\"prendaId\":\"" + prenda
								+ "\",\"descripcion\":\"Unidad 2\",\"llego\":true,\"estado\":\"BIEN\"}]}"))
				.andExpect(status().isCreated());
		mvc.perform(get("/api/v1/rentas").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + renta + "' && @.estado == 'DEVUELTA')]").exists());
	}

	@Test
	void pieza_perdida_no_cierra_la_renta_hasta_que_se_cobra_la_reposicion() throws Exception {
		UUID renta = rentaDePrueba();

		// 1ª devolución: la pieza NO llegó y se declara PERDIDA, sin cobrar. La renta sigue ACTIVA (RF-5.5).
		mvc.perform(post("/api/v1/devoluciones").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rentaId\":\"" + renta + "\",\"deposito\":100.00,\"cargoPorDanos\":0,"
								+ "\"cargoPorRetraso\":0,\"piezas\":[{\"prendaId\":\"" + prenda
								+ "\",\"descripcion\":\"Camisa\",\"llego\":false,\"estado\":\"PERDIDA\",\"perdidaCobrada\":false}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.piezas[0].resuelta").value(false));
		mvc.perform(get("/api/v1/rentas").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + renta + "' && @.estado == 'ACTIVA')]").exists());
		// La unidad sigue afuera (pendiente): no se movió a dañada/perdida en inventario todavía.
		mvc.perform(get("/api/v1/prendas/{id}/grupos-stock", prenda).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].perdidas").value(0));

		// 2ª devolución: se marca la pérdida como COBRADA (reposición 80). Ahora sí queda resuelta y DEVUELTA.
		mvc.perform(post("/api/v1/devoluciones").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rentaId\":\"" + renta + "\",\"deposito\":100.00,\"cargoPorDanos\":80.00,"
								+ "\"cargoPorRetraso\":0,\"piezas\":[{\"prendaId\":\"" + prenda
								+ "\",\"descripcion\":\"Camisa\",\"llego\":false,\"estado\":\"PERDIDA\",\"perdidaCobrada\":true}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.remanente").value(20.00))
				.andExpect(jsonPath("$.piezas[0].resuelta").value(true));
		mvc.perform(get("/api/v1/rentas").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + renta + "' && @.estado == 'DEVUELTA')]").exists());
		// La reposición cobrada sí sacó la unidad de stock (perdida).
		mvc.perform(get("/api/v1/prendas/{id}/grupos-stock", prenda).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].perdidas").value(1));
	}

	@Test
	void devolver_mas_unidades_de_las_rentadas_devuelve_400() throws Exception {
		UUID renta = rentaDeDosUnidades();

		// Se intentan revisar 3 piezas de una prenda que solo tiene 2 unidades rentadas -> 400 (RF-5.5).
		mvc.perform(post("/api/v1/devoluciones").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rentaId\":\"" + renta + "\",\"deposito\":100.00,\"cargoPorDanos\":0,"
								+ "\"cargoPorRetraso\":0,\"piezas\":["
								+ "{\"prendaId\":\"" + prenda + "\",\"descripcion\":\"1\",\"llego\":true,\"estado\":\"BIEN\"},"
								+ "{\"prendaId\":\"" + prenda + "\",\"descripcion\":\"2\",\"llego\":true,\"estado\":\"BIEN\"},"
								+ "{\"prendaId\":\"" + prenda + "\",\"descripcion\":\"3\",\"llego\":true,\"estado\":\"BIEN\"}]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void la_devolucion_deja_una_traza_legible_en_auditoria_con_el_nombre_del_cliente() throws Exception {
		UUID renta = rentaDePrueba(); // el cliente se llama "Cliente"

		mvc.perform(post("/api/v1/devoluciones").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rentaId\":\"" + renta + "\",\"deposito\":100.00,\"cargoPorDanos\":0,"
								+ "\"cargoPorRetraso\":0,\"piezas\":[{\"prendaId\":\"" + prenda
								+ "\",\"descripcion\":\"Camisa\",\"llego\":true,\"estado\":\"BIEN\"}]}"))
				.andExpect(status().isCreated());

		// RF-0.5: el detalle referencia al cliente por su nombre, no por un id opaco.
		mvc.perform(get("/api/v1/auditoria").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.accion == 'DEVOLUCION_REGISTRADA')].detalle",
						org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("«Cliente»"))));
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/devoluciones")).andExpect(status().isUnauthorized());
	}
}
