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
				.andExpect(jsonPath("$[?(@.id == '" + prenda + "')].valorReposicion").value(300.00));
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
				.andExpect(jsonPath("$[?(@.nombre == 'Camisa pirata roja')]").exists());
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
						.content("{\"sucursalId\":\"" + UUID.randomUUID() + "\",\"combinacion\":[],\"cantidadInicial\":3}"))
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
				.andExpect(jsonPath("$[?(@.nombre == '" + nombreExclusivo + "')]").doesNotExist());
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
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/prendas")).andExpect(status().isUnauthorized());
	}
}
