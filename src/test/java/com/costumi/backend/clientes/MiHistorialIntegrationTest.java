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

/**
 * "Mis Pedidos" del cliente (RF-14.4/18.9): historial paginado, por pestaña, con saldo/estado de pago por
 * operación, y detalle de una operación. Se resuelve por el usuario del token: solo ve las suyas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MiHistorialIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void el_historial_pagina_y_trae_saldo_y_estado_de_pago() throws Exception {
		Escenario e = montarRentaDevueltaConMulta();

		mvc.perform(get("/api/v1/clientes/me/historial").header("Authorization", "Bearer " + e.cliente()))
				.andExpect(status().isOk())
				// Forma paginada (objeto con contenido/total/…), no un array crudo.
				.andExpect(jsonPath("$.total").value(1))
				.andExpect(jsonPath("$.contenido.length()").value(1))
				.andExpect(jsonPath("$.contenido[0].tipo").value("RENTA"))
				.andExpect(jsonPath("$.contenido[0].empresaNombre").value(e.tiendaNombre()))
				.andExpect(jsonPath("$.contenido[0].codigoRetiro").isNotEmpty())
				// Debe importe + multa (100), sin cobros → PENDIENTE (que ya implica saldo > 0).
				.andExpect(jsonPath("$.contenido[0].saldoPendiente").isNotEmpty())
				.andExpect(jsonPath("$.contenido[0].estadoPago").value("PENDIENTE"))
				// Trae el detalle de líneas (QUÉ rentó).
				.andExpect(jsonPath("$.contenido[0].lineas[0].nombre").value("Traje"));
	}

	@Test
	void el_filtro_por_pestana_separa_las_operaciones() throws Exception {
		Escenario e = montarRentaDevueltaConMulta();
		String token = "Bearer " + e.cliente();

		// La renta devuelta con saldo cae en POR_PAGAR y en ACTIVOS (en curso, aún no cerrada)...
		mvc.perform(get("/api/v1/clientes/me/historial").param("filtro", "POR_PAGAR").header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
		mvc.perform(get("/api/v1/clientes/me/historial").param("filtro", "ACTIVOS").header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
		// ...y NO en CERRADOS ni en POR_RETIRAR.
		mvc.perform(get("/api/v1/clientes/me/historial").param("filtro", "CERRADOS").header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
		mvc.perform(get("/api/v1/clientes/me/historial").param("filtro", "POR_RETIRAR").header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
	}

	@Test
	void el_detalle_de_una_operacion_propia_se_ve_y_la_ajena_da_404() throws Exception {
		Escenario e = montarRentaDevueltaConMulta();

		mvc.perform(get("/api/v1/clientes/me/operaciones/{id}", e.operacionId())
						.header("Authorization", "Bearer " + e.cliente()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.operacionId").value(e.operacionId().toString()))
				.andExpect(jsonPath("$.tipo").value("RENTA"))
				.andExpect(jsonPath("$.lineas[0].nombre").value("Traje"));

		// Un id que no existe → 404.
		mvc.perform(get("/api/v1/clientes/me/operaciones/{id}", UUID.randomUUID())
						.header("Authorization", "Bearer " + e.cliente()))
				.andExpect(status().isNotFound());

		// La operación de otro cliente no es visible (se resuelve por el usuario del token) → 404.
		String ajeno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);
		mvc.perform(get("/api/v1/clientes/me/operaciones/{id}", e.operacionId())
						.header("Authorization", "Bearer " + ajeno))
				.andExpect(status().isNotFound());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/clientes/me/historial")).andExpect(status().isUnauthorized());
	}

	private record Escenario(String cliente, String tiendaNombre, UUID operacionId) {
	}

	/** Tienda + cliente del marketplace con ficha + una renta suya devuelta con multa (saldo pendiente). */
	private Escenario montarRentaDevueltaConMulta() throws Exception {
		String tiendaNombre = "Tienda " + UUID.randomUUID();
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + tiendaNombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());

		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());

		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"Centro\"}");
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Traje\",\"tipoArticulo\":\"RENTA\",\"precioRenta\":40.00}");
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno,
				"{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":5}");

		// El cliente del marketplace: su ficha se crea al tocar el carrito de esa tienda.
		String cliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);
		mvc.perform(get("/api/v1/carritos").header("Authorization", "Bearer " + cliente)
				.param("empresaId", empresa.toString())
				.param("sucursalId", sucursal.toString())
				.param("tipo", "RENTA"));
		UUID ficha = UUID.fromString(json.readTree(mvc.perform(get("/api/v1/clientes")
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
				.get("contenido").get(0).get("id").asText());

		UUID renta = postId("/api/v1/rentas", dueno, "{\"sucursalId\":\"" + sucursal + "\",\"clienteId\":\"" + ficha
				+ "\",\"prendaId\":\"" + prenda + "\",\"fechaRetiro\":\"2026-08-01\""
				+ ",\"fechaDevolucion\":\"2026-08-04\",\"precioPorDia\":20.00,\"deposito\":50.00}");
		mvc.perform(post("/api/v1/rentas/{id}/entregar", renta).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk());
		mvc.perform(post("/api/v1/devoluciones").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rentaId\":\"" + renta + "\",\"deposito\":50.00,\"cargoPorDanos\":150.00,"
								+ "\"cargoPorRetraso\":0.00,\"piezas\":[{\"prendaId\":\"" + prenda
								+ "\",\"descripcion\":\"Traje\",\"llego\":true,\"estado\":\"DANADA\"}]}"))
				.andExpect(status().isCreated());

		return new Escenario(cliente, tiendaNombre, renta);
	}

	private UUID postId(String path, String token, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}
}
