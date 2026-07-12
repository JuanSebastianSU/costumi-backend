package com.costumi.backend.inventario;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
	SucursalRepository sucursales;

	@Autowired
	PasswordEncoder passwordEncoder;

	private UUID crearEmpresa(String nombre) throws Exception {
		String body = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	/** Sucursal activa real de la empresa (RF-18.2): el stock exige una sucursal existente y activa (SEC-1). */
	private UUID sucursalDe(UUID empresaId) {
		return AuthTestHelper.sucursal(sucursales, empresaId);
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

	private UUID crearGrupo(String token, UUID prendaId, UUID sucursal, String combinacionJson, int cantidad)
			throws Exception {
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
		UUID grupo = crearGrupo(dueno, prenda, sucursalDe(empresa), "[]", 5);

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
		UUID grupo = crearGrupo(dueno, prenda, sucursalDe(empresa), combinacion(color, rojo), 8);

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
		UUID sucursal = sucursalDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa"));
		UUID color = crearTipoVariante(dueno, "Color");
		UUID rojo = agregarValor(dueno, color, "Rojo");
		crearGrupo(dueno, prenda, sucursal, combinacion(color, rojo), 5);

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
						.content("{\"sucursalId\":\"" + sucursalDe(empresa) + "\",\"combinacion\":"
								+ combinacion(color, valorDeTalla) + ",\"cantidadInicial\":3}"))
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
						.content("{\"sucursalId\":\"" + sucursalDe(empresa) + "\",\"combinacion\":"
								+ combinacion(material, algodon) + ",\"cantidadInicial\":3}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void variante_unica_sin_combinacion_es_valida() throws Exception {
		UUID empresa = crearEmpresa("Empresa Unica");
		String dueno = duenoDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa"));

		crearGrupo(dueno, prenda, sucursalDe(empresa), "[]", 4);
	}

	@Test
	void mover_mas_de_las_que_hay_devuelve_400() throws Exception {
		UUID empresa = crearEmpresa("Empresa Stock2");
		String dueno = duenoDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa"));
		UUID grupo = crearGrupo(dueno, prenda, sucursalDe(empresa), "[]", 2);

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

		UUID empresaB = crearEmpresa("Stock B");
		String duenoB = duenoDe(empresaB);
		mvc.perform(post("/api/v1/prendas/{prendaId}/grupos-stock", prendaDeA)
						.header("Authorization", "Bearer " + duenoB)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursalDe(empresaB) + "\",\"cantidadInicial\":5}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void mover_un_grupo_de_otra_empresa_devuelve_404() throws Exception {
		// A crea un grupo con stock.
		UUID empresaA = crearEmpresa("Mover A");
		String duenoA = duenoDe(empresaA);
		UUID prendaDeA = crearPrenda(duenoA, crearCategoria(duenoA, "Camisa"));
		UUID grupoDeA = crearGrupo(duenoA, prendaDeA, sucursalDe(empresaA), "[]", 5);

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
		UUID empresa = crearEmpresa("Reabastecer");
		String dueno = duenoDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa"));
		UUID grupo = crearGrupo(dueno, prenda, sucursalDe(empresa), "[]", 2);

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
		UUID sucursalA = sucursalDe(empresa);
		UUID sucursalB = sucursalDe(empresa);

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
		UUID sucursalOrigen = sucursalDe(empresa);
		UUID grupoOrigen = crearGrupo(dueno, prenda, sucursalOrigen, "[]", 10);
		UUID sucursalDestino = sucursalDe(empresa);

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
				.andExpect(jsonPath("$[?(@.sucursalId == '" + sucursalOrigen + "')].disponibles",
						org.hamcrest.Matchers.hasItem(7)));
	}

	@Test
	void transferir_mas_de_lo_disponible_devuelve_400() throws Exception {
		UUID empresa = crearEmpresa("Empresa TransferMax " + UUID.randomUUID());
		String dueno = duenoDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa " + UUID.randomUUID()));
		UUID grupoOrigen = crearGrupo(dueno, prenda, sucursalDe(empresa), "[]", 2);

		mvc.perform(post("/api/v1/grupos-stock/{grupoId}/transferir", grupoOrigen)
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalDestinoId\":\"" + sucursalDe(empresa) + "\",\"cantidad\":5}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void crear_grupo_en_sucursal_inexistente_devuelve_400() throws Exception {
		// SEC-1: no se puede anclar stock a una sucursal que no existe (ni a la de otra empresa: para este
		// tenant simplemente "no existe") -> referencia colgante rechazada.
		UUID empresa = crearEmpresa("Suc Inexistente " + UUID.randomUUID());
		String dueno = duenoDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa"));

		mvc.perform(post("/api/v1/prendas/{prendaId}/grupos-stock", prenda).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + UUID.randomUUID() + "\",\"combinacion\":[],\"cantidadInicial\":3}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void crear_grupo_en_sucursal_archivada_devuelve_400() throws Exception {
		// SEC-1: una sucursal archivada no admite stock nuevo (no se puede "revivir" por la API).
		UUID empresa = crearEmpresa("Suc Archivada " + UUID.randomUUID());
		String dueno = duenoDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa"));
		UUID sucursal = sucursalDe(empresa);
		var s = sucursales.buscarPorId(sucursal).orElseThrow();
		s.archivar();
		sucursales.guardar(s);

		mvc.perform(post("/api/v1/prendas/{prendaId}/grupos-stock", prenda).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":3}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void borrar_un_grupo_vacio_que_no_es_el_ultimo_devuelve_204() throws Exception {
		UUID empresa = crearEmpresa("Grupo Borrar " + UUID.randomUUID());
		String dueno = duenoDe(empresa);
		UUID sucursal = sucursalDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa"));
		UUID color = crearTipoVariante(dueno, "Color " + UUID.randomUUID());
		UUID rojo = agregarValor(dueno, color, "Rojo");
		UUID azul = agregarValor(dueno, color, "Azul");
		UUID grupoA = crearGrupo(dueno, prenda, sucursal, combinacion(color, rojo), 0);
		crearGrupo(dueno, prenda, sucursal, combinacion(color, azul), 0); // otro grupo de la misma prenda+sucursal

		mvc.perform(delete("/api/v1/grupos-stock/{id}", grupoA).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/v1/prendas/{prendaId}/grupos-stock", prenda).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == '" + grupoA + "')]").doesNotExist());
	}

	@Test
	void borrar_el_ultimo_grupo_de_la_prenda_en_la_sucursal_devuelve_409() throws Exception {
		UUID empresa = crearEmpresa("Grupo Ultimo " + UUID.randomUUID());
		String dueno = duenoDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa"));
		UUID grupo = crearGrupo(dueno, prenda, sucursalDe(empresa), "[]", 0); // único grupo, vacío

		mvc.perform(delete("/api/v1/grupos-stock/{id}", grupo).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isConflict());
	}

	@Test
	void borrar_un_grupo_con_unidades_devuelve_409() throws Exception {
		UUID empresa = crearEmpresa("Grupo Con Stock " + UUID.randomUUID());
		String dueno = duenoDe(empresa);
		UUID sucursal = sucursalDe(empresa);
		UUID prenda = crearPrenda(dueno, crearCategoria(dueno, "Camisa"));
		UUID color = crearTipoVariante(dueno, "Color " + UUID.randomUUID());
		UUID rojo = agregarValor(dueno, color, "Rojo");
		UUID azul = agregarValor(dueno, color, "Azul");
		UUID conStock = crearGrupo(dueno, prenda, sucursal, combinacion(color, rojo), 5); // tiene unidades
		crearGrupo(dueno, prenda, sucursal, combinacion(color, azul), 0); // no es el último, aísla la guarda de unidades

		mvc.perform(delete("/api/v1/grupos-stock/{id}", conStock).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isConflict());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/prendas/{prendaId}/grupos-stock", UUID.randomUUID()))
				.andExpect(status().isUnauthorized());
	}
}
