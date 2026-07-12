package com.costumi.backend.clientes;

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

/** Clientes (RF-7): alta, búsqueda, lista negra y aislamiento por tenant. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ClienteIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private UUID crearEmpresa(String nombre) throws Exception {
		String body = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	private String token(UUID empresaId, Rol rol) throws Exception {
		return AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresaId, rol);
	}

	private UUID crearCliente(String tk, String nombre, String documento) throws Exception {
		String body = mvc.perform(post("/api/v1/clientes").header("Authorization", "Bearer " + tk)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\",\"documento\":\"" + documento + "\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	private UUID postId(String path, String tk, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + tk)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	@Test
	void historial_del_cliente_y_filtro_de_pendientes() throws Exception {
		UUID empresa = crearEmpresa("Cli Hist " + UUID.randomUUID());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		String dueno = token(empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"Centro\"}");
		UUID cliente = crearCliente(dueno, "Cliente", "DOC-" + UUID.randomUUID());
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Traje\",\"tipoArticulo\":\"RENTA\",\"precioRenta\":20.00}");
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno,
				"{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":2}");
		UUID renta = postId("/api/v1/rentas", dueno, "{\"sucursalId\":\"" + sucursal + "\",\"clienteId\":\"" + cliente
				+ "\",\"prendaId\":\"" + prenda + "\",\"fechaRetiro\":\"2026-09-01\",\"fechaDevolucion\":\"2026-09-03\","
				+ "\"precioPorDia\":20.00,\"deposito\":100.00}");
		mvc.perform(post("/api/v1/rentas/{id}/entregar", renta).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk());

		// RF-7.2: el historial muestra su renta.
		mvc.perform(get("/api/v1/clientes/{id}/historial", cliente).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].tipo").value("RENTA"))
				.andExpect(jsonPath("$[0].estado").value("ACTIVA"));

		// RF-11.5/11.6: con la renta ACTIVA, el cliente sale en el filtro de pendientes.
		mvc.perform(get("/api/v1/clientes").param("conPendientes", "true").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + cliente + "')]").exists());
	}

	@Test
	void crear_buscar_por_documento_y_ponerlo_en_lista_negra() throws Exception {
		UUID empresa = crearEmpresa("Empresa Cli");
		String mostrador = token(empresa, Rol.MOSTRADOR);
		String doc = "CC-" + UUID.randomUUID();
		UUID cliente = crearCliente(mostrador, "Juan Pérez", doc);

		mvc.perform(get("/api/v1/clientes").param("buscar", doc).header("Authorization", "Bearer " + mostrador))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.documento == '" + doc + "')]").exists());

		String encargado = token(empresa, Rol.ENCARGADO);
		mvc.perform(post("/api/v1/clientes/{id}/lista-negra", cliente).header("Authorization", "Bearer " + encargado)
						.contentType(MediaType.APPLICATION_JSON).content("{\"enListaNegra\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enListaNegra").value(true));
	}

	@Test
	void una_empresa_no_ve_los_clientes_de_otra() throws Exception {
		UUID empresaA = crearEmpresa("Cli A");
		String duenoA = token(empresaA, Rol.DUENO);
		crearCliente(duenoA, "Cliente-A-" + UUID.randomUUID(), "DOC-A");

		UUID empresaB = crearEmpresa("Cli B");
		String duenoB = token(empresaB, Rol.DUENO);
		mvc.perform(get("/api/v1/clientes").header("Authorization", "Bearer " + duenoB))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.documento == 'DOC-A')]").doesNotExist());
	}

	@Test
	void un_rol_sin_permiso_no_puede_crear_cliente_403() throws Exception {
		UUID empresa = crearEmpresa("Cli Bodega");
		String bodega = token(empresa, Rol.BODEGA);

		mvc.perform(post("/api/v1/clientes").header("Authorization", "Bearer " + bodega)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"X\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void el_listado_de_clientes_se_pagina() throws Exception {
		UUID empresa = crearEmpresa("Cli Pagina " + UUID.randomUUID());
		String dueno = token(empresa, Rol.DUENO);
		crearCliente(dueno, "Ana", "DOC-" + UUID.randomUUID());
		crearCliente(dueno, "Bruno", "DOC-" + UUID.randomUUID());
		crearCliente(dueno, "Carla", "DOC-" + UUID.randomUUID());

		// Primera página de tamaño 2: 2 elementos, total 3, 2 páginas.
		mvc.perform(get("/api/v1/clientes").param("tamano", "2").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido.length()").value(2))
				.andExpect(jsonPath("$.total").value(3))
				.andExpect(jsonPath("$.totalPaginas").value(2));

		// Segunda página: el elemento restante.
		mvc.perform(get("/api/v1/clientes").param("tamano", "2").param("pagina", "1")
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido.length()").value(1))
				.andExpect(jsonPath("$.pagina").value(1));
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/clientes")).andExpect(status().isUnauthorized());
	}
}
