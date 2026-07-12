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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prendas (RF-2): alta con reglas de precio y aislamiento por tenant. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PrendaIntegrationTest {

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
		String body = mvc.perform(post("/api/v1/empresas")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	private String duenoDe(UUID empresaId) throws Exception {
		return AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresaId, Rol.DUENO);
	}

	private UUID crearCategoria(String token, String nombre) throws Exception {
		String body = mvc.perform(post("/api/v1/categorias")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	private UUID crearTipo(String token, String nombre) throws Exception {
		String body = mvc.perform(post("/api/v1/tipos-etiqueta").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\",\"defineVariante\":false,\"seleccionablePorCliente\":false}"))
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

	@Test
	void crear_una_prenda_con_valores_de_etiqueta() throws Exception {
		UUID empresa = crearEmpresa("Empresa Tags");
		String dueno = duenoDe(empresa);
		UUID categoria = crearCategoria(dueno, "Camisa");
		UUID tema = crearTipo(dueno, "Tema");
		UUID superheroe = agregarValor(dueno, tema, "Superhéroe");

		mvc.perform(post("/api/v1/prendas")
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoria + "\",\"nombre\":\"Traje\",\"tipoArticulo\":\"VENTA\","
								+ "\"precioVenta\":100.00,\"etiquetas\":[{\"tipoEtiquetaId\":\"" + tema
								+ "\",\"valorEtiquetaId\":\"" + superheroe + "\"}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.etiquetas[0].tipoEtiquetaId").value(tema.toString()))
				.andExpect(jsonPath("$.etiquetas[0].valorEtiquetaId").value(superheroe.toString()));
	}

	@Test
	void crear_una_prenda_con_valores_de_multa_por_reposicion_y_dano() throws Exception {
		UUID empresa = crearEmpresa("Empresa Multas");
		String dueno = duenoDe(empresa);
		UUID categoria = crearCategoria(dueno, "Traje");

		String body = mvc.perform(post("/api/v1/prendas")
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoria + "\",\"nombre\":\"Traje\",\"tipoArticulo\":\"RENTA\","
								+ "\"precioRenta\":40.00,\"valorReposicion\":300.00,\"valorDano\":45.00}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.valorReposicion").value(300.00))
				.andExpect(jsonPath("$.valorDano").value(45.00))
				.andReturn().getResponse().getContentAsString();
		UUID prenda = UUID.fromString(json.readTree(body).get("id").asText());

		// Persisten (round-trip por GET).
		mvc.perform(get("/api/v1/prendas").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + prenda + "')].valorReposicion").value(300.00));
	}

	@Test
	void etiquetar_con_un_tipo_que_no_aplica_a_la_categoria_devuelve_400() throws Exception {
		UUID empresa = crearEmpresa("Empresa Aplica");
		String dueno = duenoDe(empresa);
		UUID camisas = crearCategoria(dueno, "Camisas");
		UUID sombreros = crearCategoria(dueno, "Sombreros");
		// Tipo acotado SOLO a Sombreros.
		String tipoBody = mvc.perform(post("/api/v1/tipos-etiqueta").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"AlaAncha\",\"defineVariante\":false,\"seleccionablePorCliente\":false,"
								+ "\"categoriasQueAplica\":[\"" + sombreros + "\"]}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID ala = UUID.fromString(json.readTree(tipoBody).get("id").asText());
		UUID grande = agregarValor(dueno, ala, "Grande");

		// La prenda es una Camisa: el tipo de Sombreros no le aplica.
		mvc.perform(post("/api/v1/prendas")
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + camisas + "\",\"nombre\":\"Camisa\",\"tipoArticulo\":\"VENTA\","
								+ "\"precioVenta\":100.00,\"etiquetas\":[{\"tipoEtiquetaId\":\"" + ala
								+ "\",\"valorEtiquetaId\":\"" + grande + "\"}]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void crear_una_prenda_con_valor_de_etiqueta_de_otro_tipo_devuelve_400() throws Exception {
		UUID empresa = crearEmpresa("Empresa TagsMix");
		String dueno = duenoDe(empresa);
		UUID categoria = crearCategoria(dueno, "Camisa");
		UUID tema = crearTipo(dueno, "Tema");
		UUID material = crearTipo(dueno, "Material");
		UUID algodon = agregarValor(dueno, material, "Algodón");

		// Se pide Tema=<valor que en realidad es de Material> -> el valor no pertenece al tipo.
		mvc.perform(post("/api/v1/prendas")
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoria + "\",\"nombre\":\"Traje\",\"tipoArticulo\":\"VENTA\","
								+ "\"precioVenta\":100.00,\"etiquetas\":[{\"tipoEtiquetaId\":\"" + tema
								+ "\",\"valorEtiquetaId\":\"" + algodon + "\"}]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void crear_una_prenda_de_renta_y_listarla() throws Exception {
		UUID empresa = crearEmpresa("Empresa Inv");
		String dueno = duenoDe(empresa);
		UUID categoria = crearCategoria(dueno, "Camisa");

		mvc.perform(post("/api/v1/prendas")
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoria + "\",\"nombre\":\"Camisa pirata roja\","
								+ "\"tipoArticulo\":\"RENTA\",\"precioRenta\":50.00}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nombre").value("Camisa pirata roja"))
				.andExpect(jsonPath("$.tipoArticulo").value("RENTA"))
				.andExpect(jsonPath("$.empresaId").value(empresa.toString()));

		mvc.perform(get("/api/v1/prendas").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.nombre == 'Camisa pirata roja')]").exists());
	}

	@Test
	void editar_una_prenda_actualiza_sus_datos() throws Exception {
		UUID empresa = crearEmpresa("Empresa Editar");
		String dueno = duenoDe(empresa);
		UUID categoria = crearCategoria(dueno, "Camisa");
		String body = mvc.perform(post("/api/v1/prendas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoria + "\",\"nombre\":\"Vieja\","
								+ "\"tipoArticulo\":\"RENTA\",\"precioRenta\":40.00}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID prenda = UUID.fromString(json.readTree(body).get("id").asText());

		mvc.perform(put("/api/v1/prendas/{id}", prenda).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Renovada\",\"precioRenta\":55.00,\"valorReposicion\":300.00}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Renovada"))
				.andExpect(jsonPath("$.precioRenta").value(55.00))
				.andExpect(jsonPath("$.valorReposicion").value(300.00));
	}

	@Test
	void archivar_una_prenda_la_saca_de_la_disponibilidad_del_disfraz_y_activar_la_restituye() throws Exception {
		UUID empresa = crearEmpresa("Empresa Archivar");
		String dueno = duenoDe(empresa);
		UUID categoria = crearCategoria(dueno, "Camisa");
		String body = mvc.perform(post("/api/v1/prendas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoria + "\",\"nombre\":\"Traje\","
								+ "\"tipoArticulo\":\"RENTA\",\"precioRenta\":40.00}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID prenda = UUID.fromString(json.readTree(body).get("id").asText());
		mvc.perform(post("/api/v1/prendas/{id}/grupos-stock", prenda).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + AuthTestHelper.sucursal(sucursales, empresa)
								+ "\",\"combinacion\":[],\"cantidadInicial\":3}"))
				.andExpect(status().isCreated());
		String d = mvc.perform(post("/api/v1/disfraces").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Pirata\",\"slots\":[{\"orden\":1,\"nombre\":\"Cuerpo\","
								+ "\"ejePrenda\":\"FIJA\",\"prendaFijaId\":\"" + prenda + "\",\"opcional\":false}]}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID disfraz = UUID.fromString(json.readTree(d).get("id").asText());

		// Con la prenda activa, el disfraz está disponible.
		org.assertj.core.api.Assertions.assertThat(disponible(dueno, disfraz)).isTrue();

		// Archivar la prenda la retira de la operación: el disfraz deja de estar disponible.
		mvc.perform(post("/api/v1/prendas/{id}/archivar", prenda).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.archivada").value(true));
		org.assertj.core.api.Assertions.assertThat(disponible(dueno, disfraz)).isFalse();

		// Reactivarla la restituye.
		mvc.perform(post("/api/v1/prendas/{id}/activar", prenda).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.archivada").value(false));
		org.assertj.core.api.Assertions.assertThat(disponible(dueno, disfraz)).isTrue();
	}

	private boolean disponible(String token, UUID disfrazId) throws Exception {
		String body = mvc.perform(get("/api/v1/disfraces/{id}/disponibilidad", disfrazId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return json.readTree(body).get("disponible").asBoolean();
	}

	@Test
	void crear_prenda_con_costo_y_deposito_sugerido() throws Exception {
		UUID empresa = crearEmpresa("Empresa Costo");
		String dueno = duenoDe(empresa);
		UUID categoria = crearCategoria(dueno, "Traje");

		mvc.perform(post("/api/v1/prendas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoria + "\",\"nombre\":\"Traje\",\"tipoArticulo\":\"RENTA\","
								+ "\"precioRenta\":50.00,\"costoAdquisicion\":120.00,\"depositoSugerido\":200.00}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.costoAdquisicion").value(120.00))
				.andExpect(jsonPath("$.depositoSugerido").value(200.00));
	}

	@Test
	void una_prenda_de_renta_sin_precio_devuelve_400() throws Exception {
		UUID empresa = crearEmpresa("Empresa Inv2");
		String dueno = duenoDe(empresa);
		UUID categoria = crearCategoria(dueno, "Pantalon");

		mvc.perform(post("/api/v1/prendas")
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoria + "\",\"nombre\":\"Sin precio\","
								+ "\"tipoArticulo\":\"RENTA\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void una_empresa_no_ve_las_prendas_de_otra() throws Exception {
		UUID empresaA = crearEmpresa("Inv A");
		String duenoA = duenoDe(empresaA);
		UUID categoriaA = crearCategoria(duenoA, "Camisa");
		String nombreExclusivo = "SoloA-" + UUID.randomUUID();
		mvc.perform(post("/api/v1/prendas")
						.header("Authorization", "Bearer " + duenoA)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoriaA + "\",\"nombre\":\"" + nombreExclusivo + "\","
								+ "\"tipoArticulo\":\"VENTA\",\"precioVenta\":100.00}"))
				.andExpect(status().isCreated());

		UUID empresaB = crearEmpresa("Inv B");
		String duenoB = duenoDe(empresaB);
		mvc.perform(get("/api/v1/prendas").header("Authorization", "Bearer " + duenoB))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.nombre == '" + nombreExclusivo + "')]").doesNotExist());
	}

	@Test
	void un_rol_sin_permiso_no_puede_crear_prenda_403() throws Exception {
		UUID empresa = crearEmpresa("Inv Mostrador");
		String dueno = duenoDe(empresa);
		UUID categoria = crearCategoria(dueno, "Camisa");
		String mostrador = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.MOSTRADOR);

		mvc.perform(post("/api/v1/prendas")
						.header("Authorization", "Bearer " + mostrador)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoria + "\",\"nombre\":\"X\","
								+ "\"tipoArticulo\":\"RENTA\",\"precioRenta\":10.00}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void crear_prenda_con_categoria_de_otra_empresa_devuelve_400() throws Exception {
		String duenoA = duenoDe(crearEmpresa("Cross A"));
		UUID categoriaDeA = crearCategoria(duenoA, "Camisa");

		String duenoB = duenoDe(crearEmpresa("Cross B"));
		// B intenta crear una prenda referenciando la categoría de A (cross-ref por tenant, §5.4).
		mvc.perform(post("/api/v1/prendas")
						.header("Authorization", "Bearer " + duenoB)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoriaDeA + "\",\"nombre\":\"X\","
								+ "\"tipoArticulo\":\"VENTA\",\"precioVenta\":10.00}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void conteo_de_dependencias_cuenta_prendas_por_categoria_tipo_y_valor_y_excluye_archivadas() throws Exception {
		UUID empresa = crearEmpresa("Empresa Conteo");
		String dueno = duenoDe(empresa);
		UUID categoria = crearCategoria(dueno, "Camisa");
		UUID tema = crearTipo(dueno, "Tema");
		UUID superheroe = agregarValor(dueno, tema, "Superhéroe");

		// Dos prendas en la categoría; solo una lleva la etiqueta Tema=Superhéroe.
		UUID conTag = crearPrendaConTag(dueno, categoria, "Traje héroe", tema, superheroe);
		crearPrenda(dueno, categoria, "Camisa lisa");

		// Categoría: 2 prendas activas.
		mvc.perform(get("/api/v1/categorias/{id}/prendas/conteo", categoria).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.prendasActivas").value(2));
		// Tipo y valor de etiqueta: solo la prenda etiquetada.
		mvc.perform(get("/api/v1/tipos-etiqueta/{id}/prendas/conteo", tema).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.prendasActivas").value(1));
		mvc.perform(get("/api/v1/tipos-etiqueta/{t}/valores/{v}/prendas/conteo", tema, superheroe)
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.prendasActivas").value(1));

		// Archivar una prenda la descuenta del conteo de la categoría (no se cuentan archivadas).
		mvc.perform(post("/api/v1/prendas/{id}/archivar", conTag).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v1/categorias/{id}/prendas/conteo", categoria).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.prendasActivas").value(1));
		mvc.perform(get("/api/v1/tipos-etiqueta/{id}/prendas/conteo", tema).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.prendasActivas").value(0));
	}

	@Test
	void conteo_de_dependencias_respeta_tenant_y_rol() throws Exception {
		UUID empresaA = crearEmpresa("Conteo A");
		String duenoA = duenoDe(empresaA);
		UUID categoriaA = crearCategoria(duenoA, "Camisa");
		crearPrenda(duenoA, categoriaA, "Camisa A");

		// Otra empresa consulta la MISMA categoría (de A): no ve sus prendas -> 0 (aislamiento por tenant §5.4).
		String duenoB = duenoDe(crearEmpresa("Conteo B"));
		mvc.perform(get("/api/v1/categorias/{id}/prendas/conteo", categoriaA).header("Authorization", "Bearer " + duenoB))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.prendasActivas").value(0));

		// Un rol sin permiso de mantenimiento de taxonomía no puede consultar el conteo.
		String mostrador = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresaA, Rol.MOSTRADOR);
		mvc.perform(get("/api/v1/categorias/{id}/prendas/conteo", categoriaA).header("Authorization", "Bearer " + mostrador))
				.andExpect(status().isForbidden());
	}

	private UUID crearPrenda(String token, UUID categoria, String nombre) throws Exception {
		String body = mvc.perform(post("/api/v1/prendas").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoria + "\",\"nombre\":\"" + nombre + "\","
								+ "\"tipoArticulo\":\"VENTA\",\"precioVenta\":100.00}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	private UUID crearPrendaConTag(String token, UUID categoria, String nombre, UUID tipo, UUID valor) throws Exception {
		String body = mvc.perform(post("/api/v1/prendas").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoria + "\",\"nombre\":\"" + nombre + "\","
								+ "\"tipoArticulo\":\"VENTA\",\"precioVenta\":100.00,\"etiquetas\":[{\"tipoEtiquetaId\":\""
								+ tipo + "\",\"valorEtiquetaId\":\"" + valor + "\"}]}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	@Test
	void el_listado_de_prendas_se_pagina() throws Exception {
		UUID empresa = crearEmpresa("Empresa Pagina");
		String dueno = duenoDe(empresa);
		UUID categoria = crearCategoria(dueno, "Camisa");
		crearPrenda(dueno, categoria, "Alfa");
		crearPrenda(dueno, categoria, "Beta");
		crearPrenda(dueno, categoria, "Gamma");

		mvc.perform(get("/api/v1/prendas").param("tamano", "2").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido.length()").value(2))
				.andExpect(jsonPath("$.total").value(3))
				.andExpect(jsonPath("$.totalPaginas").value(2))
				.andExpect(jsonPath("$.contenido[0].nombre").value("Alfa"));

		mvc.perform(get("/api/v1/prendas").param("tamano", "2").param("pagina", "1")
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido.length()").value(1))
				.andExpect(jsonPath("$.contenido[0].nombre").value("Gamma"));
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/prendas")).andExpect(status().isUnauthorized());
	}
}
