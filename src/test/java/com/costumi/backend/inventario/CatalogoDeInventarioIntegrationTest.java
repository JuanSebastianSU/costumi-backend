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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Catálogo del dueño (RF-2/RF-13): entrar por categoría, filtrar por valores de etiqueta y ver el stock
 * disponible de cada prenda. Es la base para el inventario por categoría y para elegir opciones de disfraz.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CatalogoDeInventarioIntegrationTest {

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

	/** Crea una prenda de venta clasificada con una etiqueta (tipo=valor) y le carga {@code unidades} de stock. */
	private UUID crearPrendaConEtiquetaYStock(String token, UUID empresa, UUID categoria, String nombre,
			UUID tipo, UUID valor, int unidades) throws Exception {
		String body = mvc.perform(post("/api/v1/prendas").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoria + "\",\"nombre\":\"" + nombre + "\","
								+ "\"tipoArticulo\":\"VENTA\",\"precioVenta\":100.00,"
								+ "\"etiquetas\":[{\"tipoEtiquetaId\":\"" + tipo + "\",\"valorEtiquetaId\":\"" + valor + "\"}]}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID prenda = UUID.fromString(json.readTree(body).get("id").asText());
		UUID sucursal = AuthTestHelper.sucursal(sucursales, empresa);
		mvc.perform(post("/api/v1/prendas/{prendaId}/grupos-stock", prenda).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":" + unidades + "}"))
				.andExpect(status().isCreated());
		return prenda;
	}

	@Test
	void catalogo_filtra_por_categoria_y_etiqueta_y_trae_el_stock() throws Exception {
		UUID empresa = crearEmpresa("Catalogo " + UUID.randomUUID());
		String dueno = duenoDe(empresa);
		UUID camisas = crearCategoria(dueno, "Camisas " + UUID.randomUUID());
		UUID pantalones = crearCategoria(dueno, "Pantalones " + UUID.randomUUID());
		UUID color = crearTipo(dueno, "Color " + UUID.randomUUID());
		UUID rojo = agregarValor(dueno, color, "Rojo");
		UUID azul = agregarValor(dueno, color, "Azul");

		UUID camisaRoja = crearPrendaConEtiquetaYStock(dueno, empresa, camisas, "Camisa Roja", color, rojo, 5);
		crearPrendaConEtiquetaYStock(dueno, empresa, camisas, "Camisa Azul", color, azul, 3);
		crearPrendaConEtiquetaYStock(dueno, empresa, pantalones, "Pantalon Rojo", color, rojo, 7);

		// Sin filtro: las 3 prendas del tenant.
		mvc.perform(get("/api/v1/prendas/catalogo").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3));

		// Por categoría Camisas: solo las 2 camisas.
		mvc.perform(get("/api/v1/prendas/catalogo").param("categoriaId", camisas.toString())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[?(@.nombre == 'Pantalon Rojo')]").doesNotExist());

		// Camisas + Color=Rojo: solo la Camisa Roja, con su stock (5) y su etiqueta.
		mvc.perform(get("/api/v1/prendas/catalogo").param("categoriaId", camisas.toString())
						.param("etiqueta", color + ":" + rojo)
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(camisaRoja.toString()))
				.andExpect(jsonPath("$[0].unidadesDisponibles").value(5))
				.andExpect(jsonPath("$[0].etiquetas[0].tipoEtiquetaId").value(color.toString()))
				.andExpect(jsonPath("$[0].etiquetas[0].valorEtiquetaId").value(rojo.toString()));

		// Color=Rojo en cualquier categoría: Camisa Roja + Pantalon Rojo (AND por dimensión, sin categoría).
		mvc.perform(get("/api/v1/prendas/catalogo").param("etiqueta", color + ":" + rojo)
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void el_catalogo_no_ve_prendas_de_otra_empresa() throws Exception {
		UUID empresaA = crearEmpresa("Cat Cross A " + UUID.randomUUID());
		String duenoA = duenoDe(empresaA);
		UUID catA = crearCategoria(duenoA, "Camisas A");
		UUID tipoA = crearTipo(duenoA, "Color A");
		UUID valorA = agregarValor(duenoA, tipoA, "Rojo");
		crearPrendaConEtiquetaYStock(duenoA, empresaA, catA, "Camisa A", tipoA, valorA, 4);

		String duenoB = duenoDe(crearEmpresa("Cat Cross B " + UUID.randomUUID()));
		mvc.perform(get("/api/v1/prendas/catalogo").header("Authorization", "Bearer " + duenoB))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}
}
