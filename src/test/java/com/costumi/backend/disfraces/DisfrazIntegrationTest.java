package com.costumi.backend.disfraces;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.identidad.AuthTestHelper;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.SucursalRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
	SucursalRepository sucursales;

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

	private void crearGrupo(String token, UUID empresa, UUID prendaId, int cantidad) throws Exception {
		// La disponibilidad del disfraz es a nivel de empresa (agrega todas las sucursales); el stock exige
		// una sucursal existente y activa (SEC-1), así que se ancla a una sucursal real de la empresa.
		mvc.perform(post("/api/v1/prendas/{prendaId}/grupos-stock", prendaId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + AuthTestHelper.sucursal(sucursales, empresa)
								+ "\",\"combinacion\":[],\"cantidadInicial\":" + cantidad + "}"))
				.andExpect(status().isCreated());
	}

	/** Una "pieza única" es un disfraz con un solo slot fijo (ya no existe el modo UNIDAD_FIJA). */
	private UUID crearUnaPieza(String token, UUID prendaFijaId) throws Exception {
		String body = mvc.perform(post("/api/v1/disfraces").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Traje\",\"slots\":[{\"orden\":1,\"nombre\":\"Traje\","
								+ "\"ejePrenda\":\"FIJA\",\"prendaFijaId\":\"" + prendaFijaId + "\",\"opcional\":false}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.activo").value(true))
				.andExpect(jsonPath("$.slots[0].ejePrenda").value("FIJA"))
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
	void disponibilidad_de_una_pieza_deriva_del_stock() throws Exception {
		UUID empresa = crearEmpresa("Disfraz Stock");
		String dueno = duenoDe(empresa);
		UUID categoria = crearCategoria(dueno, "Traje");

		UUID conStock = crearPrenda(dueno, categoria);
		crearGrupo(dueno, empresa, conStock, 3);
		UUID disfrazDisponible = crearUnaPieza(dueno, conStock);

		UUID sinStock = crearPrenda(dueno, categoria);
		crearGrupo(dueno, empresa, sinStock, 0);
		UUID disfrazNoDisponible = crearUnaPieza(dueno, sinStock);

		org.assertj.core.api.Assertions.assertThat(disponible(dueno, disfrazDisponible)).isTrue();
		org.assertj.core.api.Assertions.assertThat(disponible(dueno, disfrazNoDisponible)).isFalse();
	}

	@Test
	void por_partes_con_slot_personalizable_deriva_del_pool() throws Exception {
		UUID empresa = crearEmpresa("Disfraz Pool");
		String dueno = duenoDe(empresa);
		UUID categoria = crearCategoria(dueno, "Sombrero");
		UUID prenda = crearPrenda(dueno, categoria);
		crearGrupo(dueno, empresa, prenda, 2);

		String body = mvc.perform(post("/api/v1/disfraces").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Pirata\",\"slots\":[{\"orden\":1,"
								+ "\"nombre\":\"Sombrero\",\"ejePrenda\":\"PERSONALIZABLE\","
								+ "\"pool\":{\"categoriaId\":\"" + categoria + "\",\"etiquetasPermitidas\":[]},"
								+ "\"opcional\":false}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.slots[0].ejePrenda").value("PERSONALIZABLE"))
				.andReturn().getResponse().getContentAsString();
		UUID disfraz = UUID.fromString(json.readTree(body).get("id").asText());

		org.assertj.core.api.Assertions.assertThat(disponible(dueno, disfraz)).isTrue();
	}

	private record CtxRenta(UUID empresa, String dueno, UUID sucursal, UUID cliente) {
	}

	/** Empresa aprobada, con sucursal y cliente, y con el conteo de stock apagado (foco en la resolución). */
	private CtxRenta montarRenta(String nombre) throws Exception {
		UUID empresa = crearEmpresa(nombre + " " + UUID.randomUUID());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		String dueno = duenoDe(empresa);
		String suc = mvc.perform(post("/api/v1/empresas/" + empresa + "/sucursales")
						.header("Authorization", "Bearer " + dueno).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Centro\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID sucursal = UUID.fromString(json.readTree(suc).get("id").asText());
		String cli = mvc.perform(post("/api/v1/clientes").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Cliente\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID cliente = UUID.fromString(json.readTree(cli).get("id").asText());
		// El foco de esta prueba es la resolución del disfraz a prendas, no la disponibilidad por fechas.
		mvc.perform(put("/api/v1/configuracion").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"conteoStock\":false,\"multasActivo\":true,\"multiSucursal\":false,\"pagoEnLinea\":false}"))
				.andExpect(status().isOk());
		return new CtxRenta(empresa, dueno, sucursal, cliente);
	}

	private UUID crearDisfrazFijaMasPersonalizable(String dueno, UUID prendaFija, UUID categoriaPool) throws Exception {
		String body = mvc.perform(post("/api/v1/disfraces").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Conjunto\",\"slots\":["
								+ "{\"orden\":1,\"nombre\":\"Base\",\"ejePrenda\":\"FIJA\","
								+ "\"prendaFijaId\":\"" + prendaFija + "\",\"opcional\":false},"
								+ "{\"orden\":2,\"nombre\":\"Accesorio\",\"ejePrenda\":\"PERSONALIZABLE\","
								+ "\"pool\":{\"categoriaId\":\"" + categoriaPool + "\",\"etiquetasPermitidas\":[]},"
								+ "\"opcional\":false}]}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	@Test
	void rentar_disfraz_resuelve_slots_fijo_y_personalizable_a_una_renta() throws Exception {
		CtxRenta c = montarRenta("Rentar Disfraz");
		UUID categoria = crearCategoria(c.dueno(), "Cat " + UUID.randomUUID());
		UUID prendaBase = crearPrenda(c.dueno(), categoria);      // slot fijo
		UUID prendaAccesorio = crearPrenda(c.dueno(), categoria); // elegible del pool (misma categoría)
		UUID disfraz = crearDisfrazFijaMasPersonalizable(c.dueno(), prendaBase, categoria);

		mvc.perform(post("/api/v1/disfraces/{id}/rentar", disfraz)
						.header("Authorization", "Bearer " + c.dueno()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente() + "\","
								+ "\"fechaRetiro\":\"2026-08-01\",\"fechaDevolucion\":\"2026-08-04\",\"selecciones\":["
								+ "{\"orden\":2,\"prendaId\":\"" + prendaAccesorio + "\"}]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rentaId").exists());

		// La renta tiene 2 líneas (base fija + accesorio elegido).
		mvc.perform(get("/api/v1/rentas").param("clienteId", c.cliente().toString())
						.header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(1))
				.andExpect(jsonPath("$.contenido[0].lineas.length()").value(2));
	}

	private UUID crearDisfrazConPrecioGeneral(String dueno, UUID prendaFija, UUID categoriaPool, String precioGeneral)
			throws Exception {
		String body = mvc.perform(post("/api/v1/disfraces").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Conjunto\",\"precioRentaGeneral\":" + precioGeneral + ",\"slots\":["
								+ "{\"orden\":1,\"nombre\":\"Base\",\"ejePrenda\":\"FIJA\","
								+ "\"prendaFijaId\":\"" + prendaFija + "\",\"opcional\":false},"
								+ "{\"orden\":2,\"nombre\":\"Accesorio\",\"ejePrenda\":\"PERSONALIZABLE\","
								+ "\"pool\":{\"categoriaId\":\"" + categoriaPool + "\",\"etiquetasPermitidas\":[]},"
								+ "\"opcional\":false}]}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	@Test
	void precio_general_del_disfraz_anula_la_suma_por_prendas_en_la_renta() throws Exception {
		CtxRenta c = montarRenta("Precio General");
		UUID categoria = crearCategoria(c.dueno(), "Cat " + UUID.randomUUID());
		UUID prendaBase = crearPrenda(c.dueno(), categoria);       // 40/día
		UUID prendaAccesorio = crearPrenda(c.dueno(), categoria);  // 40/día (elegible del pool)
		// Sin general, dos prendas de 40 = 80/día × 3 días = 240. Con general 100/día = 100 × 3 = 300.
		UUID disfraz = crearDisfrazConPrecioGeneral(c.dueno(), prendaBase, categoria, "100.00");

		mvc.perform(post("/api/v1/disfraces/{id}/rentar", disfraz)
						.header("Authorization", "Bearer " + c.dueno()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente() + "\","
								+ "\"fechaRetiro\":\"2026-08-01\",\"fechaDevolucion\":\"2026-08-04\",\"selecciones\":["
								+ "{\"orden\":2,\"prendaId\":\"" + prendaAccesorio + "\"}]}"))
				.andExpect(status().isOk());

		String body = mvc.perform(get("/api/v1/rentas").param("clienteId", c.cliente().toString())
						.header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		var renta = json.readTree(body).get("contenido").get(0);
		org.assertj.core.api.Assertions.assertThat(new java.math.BigDecimal(renta.get("importe").asText()))
				.isEqualByComparingTo("300.00");
		// Las dos líneas del conjunto suman el precio general por día (100), no la suma de prendas (80).
		java.math.BigDecimal sumaPorDia = java.math.BigDecimal.ZERO;
		for (var linea : renta.get("lineas")) {
			sumaPorDia = sumaPorDia.add(new java.math.BigDecimal(linea.get("precioPorDia").asText()));
		}
		org.assertj.core.api.Assertions.assertThat(sumaPorDia).isEqualByComparingTo("100.00");
	}

	@Test
	void cliente_del_marketplace_renta_un_disfraz_personalizable() throws Exception {
		CtxRenta c = montarRenta("Cliente Renta Disfraz");
		UUID categoria = crearCategoria(c.dueno(), "Cat " + UUID.randomUUID());
		UUID prendaBase = crearPrenda(c.dueno(), categoria);        // slot fijo
		UUID prendaAccesorio = crearPrenda(c.dueno(), categoria);   // elegible del pool
		UUID disfraz = crearDisfrazFijaMasPersonalizable(c.dueno(), prendaBase, categoria);

		// Un CLIENTE del marketplace renta el disfraz para sí: manda empresaId (la tienda), sin clienteId.
		String cliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);
		mvc.perform(post("/api/v1/disfraces/{id}/rentar", disfraz)
						.header("Authorization", "Bearer " + cliente).contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"empresaId\":\"" + c.empresa() + "\","
								+ "\"fechaRetiro\":\"2026-08-01\",\"fechaDevolucion\":\"2026-08-04\",\"selecciones\":["
								+ "{\"orden\":2,\"prendaId\":\"" + prendaAccesorio + "\"}]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rentaId").exists());
	}

	@Test
	void marketplace_expone_los_disfraces_de_la_tienda_al_cliente() throws Exception {
		CtxRenta c = montarRenta("Vitrina Disfraces");
		UUID categoria = crearCategoria(c.dueno(), "Cat " + UUID.randomUUID());
		UUID prendaBase = crearPrenda(c.dueno(), categoria);
		UUID disfraz = crearDisfrazFijaMasPersonalizable(c.dueno(), prendaBase, categoria);

		// Público (sin token): el cliente lista los disfraces de la tienda.
		mvc.perform(get("/api/v1/marketplace/empresas/{e}/disfraces", c.empresa()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(disfraz.toString()));

		// Detalle: estructura completa (2 slots: fijo + personalizable) + disponibilidad derivada.
		mvc.perform(get("/api/v1/marketplace/empresas/{e}/disfraces/{d}", c.empresa(), disfraz))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.disfraz.activo").value(true))
				.andExpect(jsonPath("$.disfraz.slots.length()").value(2))
				.andExpect(jsonPath("$.disfraz.slots[1].pool.categoriaId").value(categoria.toString()))
				.andExpect(jsonPath("$.disponible").exists());
	}

	@Test
	void editar_disfraz_redefine_nombre_y_slots() throws Exception {
		String dueno = duenoDe(crearEmpresa("Editar Disfraz"));
		UUID categoria = crearCategoria(dueno, "Cat " + UUID.randomUUID());
		UUID prendaBase = crearPrenda(dueno, categoria);
		UUID prendaExtra = crearPrenda(dueno, categoria);
		UUID disfraz = crearUnaPieza(dueno, prendaBase);

		mvc.perform(put("/api/v1/disfraces/{id}", disfraz).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Renovado\",\"slots\":["
								+ "{\"orden\":1,\"nombre\":\"Base\",\"ejePrenda\":\"FIJA\",\"prendaFijaId\":\"" + prendaBase + "\",\"opcional\":false},"
								+ "{\"orden\":2,\"nombre\":\"Extra\",\"ejePrenda\":\"FIJA\",\"prendaFijaId\":\"" + prendaExtra + "\",\"opcional\":true}]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Renovado"))
				.andExpect(jsonPath("$.slots.length()").value(2));
	}

	@Test
	void archivar_saca_el_disfraz_de_la_vitrina_y_no_se_puede_rentar() throws Exception {
		CtxRenta c = montarRenta("Archivar Disfraz");
		UUID categoria = crearCategoria(c.dueno(), "Cat " + UUID.randomUUID());
		UUID prendaBase = crearPrenda(c.dueno(), categoria);
		UUID disfraz = crearUnaPieza(c.dueno(), prendaBase);

		mvc.perform(post("/api/v1/disfraces/{id}/archivar", disfraz).header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activo").value(false));

		// Ya no aparece en la vitrina pública.
		mvc.perform(get("/api/v1/marketplace/empresas/{e}/disfraces", c.empresa()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		// Y no se puede rentar mientras esté archivado.
		mvc.perform(post("/api/v1/disfraces/{id}/rentar", disfraz)
						.header("Authorization", "Bearer " + c.dueno()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente() + "\","
								+ "\"fechaRetiro\":\"2026-08-01\",\"fechaDevolucion\":\"2026-08-04\"}"))
				.andExpect(status().isBadRequest());

		// Reactivarlo lo devuelve a la vitrina.
		mvc.perform(post("/api/v1/disfraces/{id}/activar", disfraz).header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activo").value(true));
		mvc.perform(get("/api/v1/marketplace/empresas/{e}/disfraces", c.empresa()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void ruleta_lista_las_opciones_disponibles_de_un_slot_y_la_prenda_fija() throws Exception {
		UUID empresa = crearEmpresa("Ruleta " + UUID.randomUUID());
		String dueno = duenoDe(empresa);
		UUID categoria = crearCategoria(dueno, "Cat " + UUID.randomUUID());
		UUID prendaBase = crearPrenda(dueno, categoria);
		crearGrupo(dueno, empresa, prendaBase, 5);
		UUID conStock = crearPrenda(dueno, categoria);
		crearGrupo(dueno, empresa, conStock, 3);
		UUID sinStock = crearPrenda(dueno, categoria);
		crearGrupo(dueno, empresa, sinStock, 0);
		// slot 1 = fijo (prendaBase); slot 2 = personalizable (pool = toda la categoría).
		UUID disfraz = crearDisfrazFijaMasPersonalizable(dueno, prendaBase, categoria);

		// Ruleta del slot personalizable: solo las prendas del pool con stock (base=5 y conStock=3), sin la de 0.
		mvc.perform(get("/api/v1/marketplace/empresas/{e}/disfraces/{d}/slots/{o}/opciones", empresa, disfraz, 2))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ejePrenda").value("PERSONALIZABLE"))
				.andExpect(jsonPath("$.opciones.length()").value(2))
				.andExpect(jsonPath("$.opciones[?(@.prendaId == '" + conStock + "')]").exists())
				.andExpect(jsonPath("$.opciones[?(@.prendaId == '" + sinStock + "')]").doesNotExist());

		// Ruleta del slot fijo: su única prenda.
		mvc.perform(get("/api/v1/marketplace/empresas/{e}/disfraces/{d}/slots/{o}/opciones", empresa, disfraz, 1))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ejePrenda").value("FIJA"))
				.andExpect(jsonPath("$.opciones.length()").value(1))
				.andExpect(jsonPath("$.opciones[0].prendaId").value(prendaBase.toString()));
	}

	@Test
	void rentar_disfraz_con_prenda_fuera_del_pool_devuelve_400() throws Exception {
		CtxRenta c = montarRenta("Pool Invalido");
		UUID categoriaPool = crearCategoria(c.dueno(), "Pool " + UUID.randomUUID());
		UUID otraCategoria = crearCategoria(c.dueno(), "Otra " + UUID.randomUUID());
		UUID prendaBase = crearPrenda(c.dueno(), categoriaPool);
		UUID prendaFuera = crearPrenda(c.dueno(), otraCategoria); // NO pertenece al pool (otra categoría)
		UUID disfraz = crearDisfrazFijaMasPersonalizable(c.dueno(), prendaBase, categoriaPool);

		mvc.perform(post("/api/v1/disfraces/{id}/rentar", disfraz)
						.header("Authorization", "Bearer " + c.dueno()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + c.sucursal() + "\",\"clienteId\":\"" + c.cliente() + "\","
								+ "\"fechaRetiro\":\"2026-08-01\",\"fechaDevolucion\":\"2026-08-04\",\"selecciones\":["
								+ "{\"orden\":2,\"prendaId\":\"" + prendaFuera + "\"}]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void disfraz_sin_slots_devuelve_400() throws Exception {
		String dueno = duenoDe(crearEmpresa("Disfraz Vacio"));

		mvc.perform(post("/api/v1/disfraces").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Vacío\",\"slots\":[]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void un_rol_sin_permiso_no_puede_crear_disfraz_403() throws Exception {
		UUID empresa = crearEmpresa("Disfraz Rol");
		String mostrador = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.MOSTRADOR);

		mvc.perform(post("/api/v1/disfraces").header("Authorization", "Bearer " + mostrador)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"X\",\"slots\":[{\"orden\":1,\"nombre\":\"S\",\"ejePrenda\":\"FIJA\","
								+ "\"prendaFijaId\":\"" + UUID.randomUUID() + "\",\"opcional\":false}]}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void crear_disfraz_con_prenda_de_otra_empresa_devuelve_400() throws Exception {
		String duenoA = duenoDe(crearEmpresa("Disfraz Cross A"));
		UUID categoriaA = crearCategoria(duenoA, "Traje");
		UUID prendaDeA = crearPrenda(duenoA, categoriaA);

		String duenoB = duenoDe(crearEmpresa("Disfraz Cross B"));
		// B intenta un disfraz con un slot fijo apuntando a la prenda de A (cross-ref por tenant, §5.4).
		mvc.perform(post("/api/v1/disfraces").header("Authorization", "Bearer " + duenoB)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Robo\",\"slots\":[{\"orden\":1,\"nombre\":\"S\",\"ejePrenda\":\"FIJA\","
								+ "\"prendaFijaId\":\"" + prendaDeA + "\",\"opcional\":false}]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/disfraces")).andExpect(status().isUnauthorized());
	}
}
