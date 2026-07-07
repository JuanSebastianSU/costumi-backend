package com.costumi.backend.inventario;

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
 * Grupos de stock (RF-2.2, RF-2.7.3): variante = combinación real de valores de etiqueta, conteo
 * por estado, movimientos y aislamiento por tenant.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class GrupoDeStockIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	/** Sucursal donde vive el stock de cada test (RF-18.2). La misma para todo el método de prueba. */
	private final UUID sucursal = UUID.randomUUID();

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
						.content("{\"categoriaId\":\"" + categoriaId + "\",\"nombre\":\"Camisa\","
								+ "\"tipoArticulo\":\"RENTA\",\"precioRenta\":40.00}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	private UUID crearTipoVariante(String token, String nombre) throws Exception {
		String body = mvc.perform(post("/api/v1/tipos-etiqueta").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\",\"defineVariante\":true,\"seleccionablePorCliente\":false}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	private UUID agregarValor(String token, UUID tipoId, String valor) throws Exception {
		String body = mvc.perform(post("/api/v1/tipos-etiqueta/{tipoId}/valores", tipoId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"valor\":\"" + valor + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	private static String combinacion(UUID tipoId, UUID valorId) {
		return "[{\"tipoEtiquetaId\":\"" + tipoId + "\",\"valorEtiquetaId\":\"" + valorId + "\"}]";
	}

	private UUID crearGrupo(String token, UUID prendaId, String combinacionJson, int cantidad) throws Exception {
		String body = mvc.perform(post("/api/v1/prendas/{prendaId}/grupos-stock", prendaId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":" + combinacionJson
								+ ",\"cantidadInicial\":" + cantidad + "}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.disponibles").value(cantidad))
				.andExpect(jsonPath("$.total").value(cantidad))
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	@Test
	void ajuste_de_stock_con_motivo_corrige_y_queda_auditado() throws Exception {
		UUID empresa = crearEmpresa("Empresa Ajuste " + UUID.randomUUID());
		String dueno = duenoDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa " + UUID.randomUUID()));
		UUID grupo = crearGrupo(dueno, prenda, "[]", 5);

		// Ajuste con motivo: -2 disponibles (RF-10).
		mvc.perform(post("/api/v1/grupos-stock/{id}/ajuste", grupo).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"estado\":\"DISPONIBLE\",\"delta\":-2,\"motivo\":\"merma por conteo\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.disponibles").value(3));

		// Un ajuste que dejaría el conteo en negativo -> 400.
		mvc.perform(post("/api/v1/grupos-stock/{id}/ajuste", grupo).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"estado\":\"DISPONIBLE\",\"delta\":-10,\"motivo\":\"x\"}"))
				.andExpect(status().isBadRequest());

		// El ajuste quedó auditado (RF-10 + RF-0.5).
		mvc.perform(get("/api/v1/auditoria").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.accion == 'STOCK_AJUSTADO')]").exists());
	}

	@Test
	void crear_con_combinacion_real_listar_y_mover_unidades() throws Exception {
		UUID empresa = crearEmpresa("Empresa Stock");
		String dueno = duenoDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa"));
		UUID color = crearTipoVariante(dueno, "Color");
		UUID rojo = agregarValor(dueno, color, "Rojo");
		UUID grupo = crearGrupo(dueno, prenda, combinacion(color, rojo), 8);

		mvc.perform(get("/api/v1/prendas/{prendaId}/grupos-stock", prenda).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == '" + grupo + "')]").exists())
				.andExpect(jsonPath("$[0].combinacion[0].tipoEtiquetaId").value(color.toString()))
				.andExpect(jsonPath("$[0].combinacion[0].valorEtiquetaId").value(rojo.toString()));

		mvc.perform(post("/api/v1/grupos-stock/{grupoId}/mover", grupo)
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"desde\":\"DISPONIBLE\",\"hacia\":\"DANADA\",\"cantidad\":3}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.disponibles").value(5))
				.andExpect(jsonPath("$.danadas").value(3))
				.andExpect(jsonPath("$.total").value(8));
	}

	@Test
	void dos_grupos_con_la_misma_combinacion_devuelve_409() throws Exception {
		UUID empresa = crearEmpresa("Empresa Dup");
		String dueno = duenoDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa"));
		UUID color = crearTipoVariante(dueno, "Color");
		UUID rojo = agregarValor(dueno, color, "Rojo");
		crearGrupo(dueno, prenda, combinacion(color, rojo), 5);

		mvc.perform(post("/api/v1/prendas/{prendaId}/grupos-stock", prenda)
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":" + combinacion(color, rojo)
								+ ",\"cantidadInicial\":3}"))
				.andExpect(status().isConflict());
	}

	@Test
	void combinacion_con_valor_de_otro_tipo_devuelve_400() throws Exception {
		UUID empresa = crearEmpresa("Empresa Mix");
		String dueno = duenoDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa"));
		UUID color = crearTipoVariante(dueno, "Color");
		UUID talla = crearTipoVariante(dueno, "Talla");
		UUID valorDeTalla = agregarValor(dueno, talla, "M");

		// Se pide Color=<valor que en realidad es de Talla> -> el valor no pertenece al tipo.
		mvc.perform(post("/api/v1/prendas/{prendaId}/grupos-stock", prenda)
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":" + combinacion(color, valorDeTalla)
								+ ",\"cantidadInicial\":3}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void combinacion_con_tipo_que_no_define_variante_devuelve_400() throws Exception {
		UUID empresa = crearEmpresa("Empresa NoVar");
		String dueno = duenoDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa"));
		// Tipo con defineVariante=false.
		String tipoBody = mvc.perform(post("/api/v1/tipos-etiqueta").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Material\",\"defineVariante\":false,\"seleccionablePorCliente\":false}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID material = UUID.fromString(json.readTree(tipoBody).get("id").asText());
		UUID algodon = agregarValor(dueno, material, "Algodón");

		mvc.perform(post("/api/v1/prendas/{prendaId}/grupos-stock", prenda)
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":" + combinacion(material, algodon)
								+ ",\"cantidadInicial\":3}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void variante_unica_sin_combinacion_es_valida() throws Exception {
		UUID empresa = crearEmpresa("Empresa Unica");
		String dueno = duenoDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa"));

		crearGrupo(dueno, prenda, "[]", 4);
	}

	@Test
	void mover_mas_de_las_que_hay_devuelve_400() throws Exception {
		UUID empresa = crearEmpresa("Empresa Stock2");
		String dueno = duenoDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa"));
		UUID grupo = crearGrupo(dueno, prenda, "[]", 2);

		mvc.perform(post("/api/v1/grupos-stock/{grupoId}/mover", grupo)
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"desde\":\"DISPONIBLE\",\"hacia\":\"PERDIDA\",\"cantidad\":5}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void crear_grupo_en_prenda_de_otra_empresa_devuelve_404() throws Exception {
		String duenoA = duenoDe(crearEmpresa("Stock A"));
		UUID prendaDeA = crearPrenda(duenoA, crearCategoria(duenoA, "Camisa"));

		String duenoB = duenoDe(crearEmpresa("Stock B"));
		mvc.perform(post("/api/v1/prendas/{prendaId}/grupos-stock", prendaDeA)
						.header("Authorization", "Bearer " + duenoB)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"cantidadInicial\":5}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void mover_un_grupo_de_otra_empresa_devuelve_404() throws Exception {
		// A crea un grupo con stock.
		String duenoA = duenoDe(crearEmpresa("Mover A"));
		UUID prendaDeA = crearPrenda(duenoA, crearCategoria(duenoA, "Camisa"));
		UUID grupoDeA = crearGrupo(duenoA, prendaDeA, "[]", 5);

		// B intenta cargar por PK y mover ese grupo: el find() forzado lo bloquea (§5.4) -> 404.
		String duenoB = duenoDe(crearEmpresa("Mover B"));
		mvc.perform(post("/api/v1/grupos-stock/{grupoId}/mover", grupoDeA)
						.header("Authorization", "Bearer " + duenoB)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"desde\":\"DISPONIBLE\",\"hacia\":\"DANADA\",\"cantidad\":1}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void reabastecer_aumenta_el_stock_y_stock_bajo_lo_lista() throws Exception {
		String dueno = duenoDe(crearEmpresa("Reabastecer"));
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa"));
		UUID grupo = crearGrupo(dueno, prenda, "[]", 2);

		// Con umbral 5, el grupo (2 disponibles) sale en stock bajo.
		mvc.perform(get("/api/v1/grupos-stock/stock-bajo").param("umbral", "5")
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == '" + grupo + "')]").exists());

		// Entrada de 10 unidades -> 12 disponibles.
		mvc.perform(post("/api/v1/grupos-stock/{grupoId}/entrada", grupo).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":10}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.disponibles").value(12));

		// Ya no está bajo el umbral 5.
		mvc.perform(get("/api/v1/grupos-stock/stock-bajo").param("umbral", "5")
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == '" + grupo + "')]").doesNotExist());
	}

	@Test
	void la_misma_variante_en_otra_sucursal_es_un_grupo_aparte() throws Exception {
		// RF-18.2: el stock se lleva por sucursal, así que la misma prenda+variante puede existir en
		// dos sucursales sin colisionar (la clave de duplicado es por sucursal, no por empresa).
		UUID empresa = crearEmpresa("Empresa MultiSucursal " + UUID.randomUUID());
		String dueno = duenoDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa " + UUID.randomUUID()));
		UUID sucursalA = UUID.randomUUID();
		UUID sucursalB = UUID.randomUUID();

		mvc.perform(post("/api/v1/prendas/{prendaId}/grupos-stock", prenda).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursalA + "\",\"combinacion\":[],\"cantidadInicial\":4}"))
				.andExpect(status().isCreated());

		// Misma prenda y misma variante (única), pero en otra sucursal: se permite (no es duplicado).
		mvc.perform(post("/api/v1/prendas/{prendaId}/grupos-stock", prenda).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursalB + "\",\"combinacion\":[],\"cantidadInicial\":7}"))
				.andExpect(status().isCreated());

		// La prenda queda con dos grupos (uno por sucursal).
		mvc.perform(get("/api/v1/prendas/{prendaId}/grupos-stock", prenda).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void transferir_stock_a_otra_sucursal_mueve_las_unidades() throws Exception {
		// RF-10.3: transferir 3 unidades del grupo (sucursal origen) crea/alimenta el grupo de la misma
		// variante en la sucursal de destino y descuenta el origen.
		UUID empresa = crearEmpresa("Empresa Transfer " + UUID.randomUUID());
		String dueno = duenoDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa " + UUID.randomUUID()));
		UUID grupoOrigen = crearGrupo(dueno, prenda, "[]", 10); // vive en la sucursal del campo `sucursal`
		UUID sucursalDestino = UUID.randomUUID();

		mvc.perform(post("/api/v1/grupos-stock/{grupoId}/transferir", grupoOrigen)
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalDestinoId\":\"" + sucursalDestino + "\",\"cantidad\":3}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(grupoOrigen.toString()))
				.andExpect(jsonPath("$.disponibles").value(7)); // el origen bajó de 10 a 7

		// La prenda ahora tiene dos grupos: origen (7) y destino (3), uno por sucursal.
		mvc.perform(get("/api/v1/prendas/{prendaId}/grupos-stock", prenda).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[?(@.sucursalId == '" + sucursalDestino + "')].disponibles",
						org.hamcrest.Matchers.hasItem(3)))
				.andExpect(jsonPath("$[?(@.sucursalId == '" + sucursal + "')].disponibles",
						org.hamcrest.Matchers.hasItem(7)));
	}

	@Test
	void transferir_mas_de_lo_disponible_devuelve_400() throws Exception {
		UUID empresa = crearEmpresa("Empresa TransferMax " + UUID.randomUUID());
		String dueno = duenoDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa " + UUID.randomUUID()));
		UUID grupoOrigen = crearGrupo(dueno, prenda, "[]", 2);

		mvc.perform(post("/api/v1/grupos-stock/{grupoId}/transferir", grupoOrigen)
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalDestinoId\":\"" + UUID.randomUUID() + "\",\"cantidad\":5}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/prendas/{prendaId}/grupos-stock", UUID.randomUUID()))
				.andExpect(status().isUnauthorized());
	}
}
