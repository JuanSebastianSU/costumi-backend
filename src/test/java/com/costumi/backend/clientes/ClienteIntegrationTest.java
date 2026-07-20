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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
		String nombreEmpresa = "Cli Hist " + UUID.randomUUID();
		UUID empresa = crearEmpresa(nombreEmpresa);
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
				.andExpect(jsonPath("$[0].estado").value("ACTIVA"))
				// La operación trae su tienda, para "Mis Pedidos" (cruza tiendas) y el reembolso.
				.andExpect(jsonPath("$[0].empresaId").value(empresa.toString()))
				.andExpect(jsonPath("$[0].empresaNombre").value(nombreEmpresa))
				// ...el detalle de artículos con nombre (para mostrar QUÉ se rentó, con imagen)...
				.andExpect(jsonPath("$[0].lineas[0].nombre").value("Traje"))
				.andExpect(jsonPath("$[0].lineas[0].cantidad").value(1))
				// ...y el código de retiro (que el cliente muestra en la tienda).
				.andExpect(jsonPath("$[0].codigoRetiro").value(org.hamcrest.Matchers.startsWith("R-")));

		// RF-11.5/11.6: con la renta ACTIVA, el cliente sale en el filtro de pendientes.
		mvc.perform(get("/api/v1/clientes").param("conPendientes", "true").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + cliente + "')]").exists());
	}

	@Test
	void filtros_de_pendientes_por_categoria_vencidas_multas_y_saldos() throws Exception {
		UUID empresa = crearEmpresa("Cli Filtros " + UUID.randomUUID());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		String dueno = token(empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"Centro\"}");
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Traje\",\"tipoArticulo\":\"RENTA\",\"precioRenta\":20.00}");
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno,
				"{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":5}");

		// Cliente A: renta con fecha de devolución en el pasado -> ACTIVA y VENCIDA; sin pagar -> con SALDO.
		UUID clienteVencido = crearCliente(dueno, "Vencido", "DOC-V-" + UUID.randomUUID());
		UUID rentaVencida = postId("/api/v1/rentas", dueno, "{\"sucursalId\":\"" + sucursal + "\",\"clienteId\":\""
				+ clienteVencido + "\",\"prendaId\":\"" + prenda + "\",\"fechaRetiro\":\"2020-01-01\","
				+ "\"fechaDevolucion\":\"2020-01-05\",\"precioPorDia\":20.00,\"deposito\":50.00}");
		mvc.perform(post("/api/v1/rentas/{id}/entregar", rentaVencida).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk());

		// Cliente B: renta entregada y devuelta con multa (daños+retraso > depósito) -> con MULTA y con SALDO.
		UUID clienteMulta = crearCliente(dueno, "Multado", "DOC-M-" + UUID.randomUUID());
		UUID rentaMulta = postId("/api/v1/rentas", dueno, "{\"sucursalId\":\"" + sucursal + "\",\"clienteId\":\""
				+ clienteMulta + "\",\"prendaId\":\"" + prenda + "\",\"fechaRetiro\":\"2026-09-01\","
				+ "\"fechaDevolucion\":\"2026-09-03\",\"precioPorDia\":20.00,\"deposito\":50.00}");
		mvc.perform(post("/api/v1/rentas/{id}/entregar", rentaMulta).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk());
		mvc.perform(post("/api/v1/devoluciones").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rentaId\":\"" + rentaMulta + "\",\"deposito\":50.00,\"cargoPorDanos\":60.00,"
								+ "\"cargoPorRetraso\":20.00,\"piezas\":[{\"prendaId\":\"" + prenda
								+ "\",\"descripcion\":\"Traje\",\"llego\":true,\"estado\":\"DANADA\",\"perdidaCobrada\":false}]}"))
				.andExpect(status().isCreated());

		// VENCIDAS: solo el cliente con renta activa fuera de plazo.
		mvc.perform(get("/api/v1/clientes").param("filtro", "VENCIDAS").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + clienteVencido + "')]").exists())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + clienteMulta + "')]").doesNotExist());

		// MULTAS: solo el cliente cuya devolución dejó multa; y su multaTotal viaja en el DTO (RF-7/11.5).
		mvc.perform(get("/api/v1/clientes").param("filtro", "MULTAS").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + clienteMulta + "')]").exists())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + clienteMulta + "' && @.multaTotal > 0)]").exists())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + clienteVencido + "')]").doesNotExist());

		// SALDOS: ambos deben dinero (ninguno pagó); el saldoPendiente viaja en el DTO.
		mvc.perform(get("/api/v1/clientes").param("filtro", "SALDOS").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + clienteVencido + "')]").exists())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + clienteMulta + "')]").exists())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + clienteMulta + "' && @.saldoPendiente > 0)]").exists());

		// PENDIENTES (indicador general): ambos aparecen (activa por devolver ∪ saldos).
		mvc.perform(get("/api/v1/clientes").param("filtro", "PENDIENTES").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + clienteVencido + "')]").exists())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + clienteMulta + "')]").exists());
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
	void editar_actualiza_los_datos_del_cliente() throws Exception {
		UUID empresa = crearEmpresa("Cli Editar " + UUID.randomUUID());
		String dueno = token(empresa, Rol.DUENO);
		UUID cliente = crearCliente(dueno, "Nombre Viejo", "DOC-" + UUID.randomUUID());

		mvc.perform(put("/api/v1/clientes/{id}", cliente).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Nombre Nuevo\",\"telefono\":\"3001234567\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Nombre Nuevo"))
				.andExpect(jsonPath("$.telefono").value("3001234567"));
	}

	@Test
	void editar_no_cambia_el_email_del_cliente_es_inmutable_tras_crear() throws Exception {
		UUID empresa = crearEmpresa("Cli Email " + UUID.randomUUID());
		String dueno = token(empresa, Rol.DUENO);
		String creado = mvc.perform(post("/api/v1/clientes").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Con Correo\",\"email\":\"original@correo.com\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("original@correo.com"))
				.andReturn().getResponse().getContentAsString();
		UUID cliente = UUID.fromString(json.readTree(creado).get("id").asText());

		// Aunque el body traiga un correo distinto, se ignora: el correo identifica la ficha y no se edita.
		mvc.perform(put("/api/v1/clientes/{id}", cliente).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Con Correo Editado\",\"email\":\"otro@correo.com\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Con Correo Editado"))
				.andExpect(jsonPath("$.email").value("original@correo.com"));
	}

	@Test
	void archivar_lo_oculta_de_la_lista_e_incluir_archivados_lo_muestra_y_activar_lo_reincorpora() throws Exception {
		UUID empresa = crearEmpresa("Cli Archivar " + UUID.randomUUID());
		String dueno = token(empresa, Rol.DUENO);
		UUID cliente = crearCliente(dueno, "Archivable", "DOC-" + UUID.randomUUID());

		mvc.perform(post("/api/v1/clientes/{id}/archivar", cliente).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.archivada").value(true));

		// Ya no aparece en la lista activa por defecto.
		mvc.perform(get("/api/v1/clientes").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + cliente + "')]").doesNotExist());

		// Pero sí con incluirArchivados=true (para poder reactivarlo).
		mvc.perform(get("/api/v1/clientes").param("incluirArchivados", "true")
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + cliente + "')]").exists());

		// Activar lo reincorpora a la lista activa.
		mvc.perform(post("/api/v1/clientes/{id}/activar", cliente).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.archivada").value(false));
		mvc.perform(get("/api/v1/clientes").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + cliente + "')]").exists());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/clientes")).andExpect(status().isUnauthorized());
	}
}
