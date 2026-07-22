package com.costumi.backend;

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
 * Las listas del backoffice tienen que seguir siendo usables cuando la empresa crece: se paginan y se
 * pueden buscar. Antes varias devolvían TODO (auditoría, notificaciones, disfraces, devoluciones,
 * reembolsos, empleados) y ninguna salvo clientes dejaba buscar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BusquedaYPaginacionIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private record Ctx(UUID empresa, String dueno, UUID categoria) {
	}

	private UUID postId(String path, String token, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	private Ctx montar() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Escala " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		return new Ctx(empresa, dueno, categoria);
	}

	@Test
	void las_prendas_se_pueden_buscar_por_nombre() throws Exception {
		Ctx c = montar();
		crearPrenda(c, "Capa Veneciana");
		crearPrenda(c, "Sombrero Pirata");

		mvc.perform(get("/api/v1/prendas").header("Authorization", "Bearer " + c.dueno()).param("buscar", "venec"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(1))
				.andExpect(jsonPath("$.contenido[0].nombre").value("Capa Veneciana"));

		// Sin texto siguen saliendo las dos.
		mvc.perform(get("/api/v1/prendas").header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(2));
	}

	@Test
	void los_disfraces_se_paginan_y_se_buscan() throws Exception {
		Ctx c = montar();
		UUID prenda = crearPrenda(c, "Base");
		crearDisfraz(c, "Traje Pirata", prenda);
		crearDisfraz(c, "Traje Veneciano", prenda);
		crearDisfraz(c, "Bruja Clasica", prenda);

		// Página de 2: el total dice que hay 3.
		mvc.perform(get("/api/v1/disfraces").header("Authorization", "Bearer " + c.dueno())
						.param("tamano", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(3))
				.andExpect(jsonPath("$.contenido.length()").value(2))
				.andExpect(jsonPath("$.totalPaginas").value(2));

		mvc.perform(get("/api/v1/disfraces").header("Authorization", "Bearer " + c.dueno())
						.param("buscar", "traje"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(2));
	}

	@Test
	void la_auditoria_se_pagina() throws Exception {
		Ctx c = montar();
		// Crear prendas deja rastro en el trail.
		crearPrenda(c, "Rastro Uno");
		crearPrenda(c, "Rastro Dos");

		mvc.perform(get("/api/v1/auditoria").header("Authorization", "Bearer " + c.dueno()).param("tamano", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido.length()").value(1))
				.andExpect(jsonPath("$.total").isNumber());
	}

	@Test
	void las_listas_que_antes_devolvian_todo_ahora_responden_paginadas() throws Exception {
		Ctx c = montar();
		for (String ruta : new String[] { "/api/v1/notificaciones", "/api/v1/devoluciones", "/api/v1/reembolsos",
				"/api/v1/empleados" }) {
			mvc.perform(get(ruta).header("Authorization", "Bearer " + c.dueno()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.contenido").isArray())
					.andExpect(jsonPath("$.total").isNumber())
					.andExpect(jsonPath("$.totalPaginas").isNumber());
		}
	}

	@Test
	void las_ventas_se_buscan_por_su_codigo_de_retiro() throws Exception {
		Ctx c = montar();
		UUID prenda = crearPrenda(c, "Para Vender");
		UUID sucursal = postId("/api/v1/empresas/" + c.empresa() + "/sucursales", c.dueno(),
				"{\"nombre\":\"Centro\"}");
		mvc.perform(post("/api/v1/prendas/{id}/grupos-stock", prenda).header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":5}"))
				.andExpect(status().isCreated());
		UUID cliente = postId("/api/v1/clientes", c.dueno(), "{\"nombre\":\"Cliente\"}");
		mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + c.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"clienteId\":\"" + cliente
								+ "\",\"lineas\":[{\"prendaId\":\"" + prenda
								+ "\",\"cantidad\":1,\"precioUnitario\":90.00}]}"))
				.andExpect(status().isCreated());

		String lista = mvc.perform(get("/api/v1/ventas").header("Authorization", "Bearer " + c.dueno()))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		String codigo = json.readTree(lista).get("contenido").get(0).get("codigoRetiro").asText();

		// El dueño escribe el código tal como lo ve el cliente (con prefijo y en mayúsculas).
		mvc.perform(get("/api/v1/ventas").header("Authorization", "Bearer " + c.dueno()).param("buscar", codigo))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(1))
				.andExpect(jsonPath("$.contenido[0].codigoRetiro").value(codigo));

		// Un código que no existe no devuelve nada (no "todo").
		mvc.perform(get("/api/v1/ventas").header("Authorization", "Bearer " + c.dueno()).param("buscar", "V-00000000"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(0));
	}

	private UUID crearPrenda(Ctx c, String nombre) throws Exception {
		return postId("/api/v1/prendas", c.dueno(), "{\"categoriaId\":\"" + c.categoria() + "\",\"nombre\":\""
				+ nombre + "\",\"tipoArticulo\":\"AMBOS\",\"precioRenta\":30.00,\"precioVenta\":90.00}");
	}

	private void crearDisfraz(Ctx c, String nombre, UUID prendaFija) throws Exception {
		postId("/api/v1/disfraces", c.dueno(), "{\"nombre\":\"" + nombre + "\",\"slots\":[{\"orden\":1,"
				+ "\"nombre\":\"Cuerpo\",\"ejePrenda\":\"FIJA\",\"prendaFijaId\":\"" + prendaFija
				+ "\",\"opcional\":false}]}");
	}
}
