package com.costumi.backend.ventas;

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

/** Ventas/POS (RF-4): total con descuento, a nombre del empleado del token. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VentaIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private String dueno;

	private UUID postId(String path, String token, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	private UUID[] montar() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Venta " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		this.dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"Centro\"}");
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Peluca " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Peluca\",\"tipoArticulo\":\"VENTA\",\"precioVenta\":50.00}");
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno,
				"{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":5}");
		return new UUID[] { sucursal, prenda };
	}

	private int disponiblesDe(UUID prenda) throws Exception {
		String res = mvc.perform(get("/api/v1/prendas/{id}/grupos-stock", prenda)
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return json.readTree(res).get(0).get("disponibles").asInt();
	}

	@Test
	void registrar_venta_con_cliente_inexistente_devuelve_400() throws Exception {
		UUID[] ctx = montar();
		UUID sucursal = ctx[0];
		UUID prenda = ctx[1];

		// SEC-2: si la venta declara un cliente, debe existir y ser de esta empresa (una venta sin cliente
		// —anónima de mostrador— sí se permite, como en los demás tests).
		mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"clienteId\":\"" + UUID.randomUUID()
								+ "\",\"lineas\":[{\"prendaId\":\"" + prenda
								+ "\",\"cantidad\":1,\"precioUnitario\":50.00}]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void registrar_venta_calcula_el_total_con_descuento() throws Exception {
		UUID[] ctx = montar();
		UUID sucursal = ctx[0];
		UUID prenda = ctx[1];

		mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"descuento\":10.00,\"lineas\":[{\"prendaId\":\""
								+ prenda + "\",\"cantidad\":2,\"precioUnitario\":50.00}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.total").value(90.00))
				.andExpect(jsonPath("$.estado").value("CONFIRMADA"))
				.andExpect(jsonPath("$.lineas.length()").value(1))
				.andExpect(jsonPath("$.empleadoId").exists());

		mvc.perform(get("/api/v1/ventas").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void confirmar_venta_descuenta_el_stock() throws Exception {
		UUID[] ctx = montar();
		UUID sucursal = ctx[0];
		UUID prenda = ctx[1];

		mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"lineas\":[{\"prendaId\":\"" + prenda
								+ "\",\"cantidad\":2,\"precioUnitario\":50.00}]}"))
				.andExpect(status().isCreated());

		// Stock 5 - 2 vendidas = 3 disponibles (RF-4.4).
		org.assertj.core.api.Assertions.assertThat(disponiblesDe(prenda)).isEqualTo(3);
	}

	@Test
	void devolver_una_venta_reingresa_el_stock_y_la_marca_devuelta() throws Exception {
		UUID[] ctx = montar();
		UUID sucursal = ctx[0];
		UUID prenda = ctx[1];

		// Vender 2 (stock 5 -> 3).
		String res = mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"lineas\":[{\"prendaId\":\"" + prenda
								+ "\",\"cantidad\":2,\"precioUnitario\":50.00}]}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID ventaId = UUID.fromString(json.readTree(res).get("id").asText());
		org.assertj.core.api.Assertions.assertThat(disponiblesDe(prenda)).isEqualTo(3);

		// RF-4.5: devolver la venta la marca DEVUELTA y reingresa el stock (3 -> 5).
		mvc.perform(post("/api/v1/ventas/{id}/devolver", ventaId).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("DEVUELTA"));
		org.assertj.core.api.Assertions.assertThat(disponiblesDe(prenda)).isEqualTo(5);

		// Una venta ya devuelta no se puede devolver otra vez -> 409.
		mvc.perform(post("/api/v1/ventas/{id}/devolver", ventaId).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isConflict());
	}

	@Test
	void devolucion_parcial_reingresa_solo_lo_devuelto_y_marca_la_venta_parcial() throws Exception {
		UUID[] ctx = montar();
		UUID sucursal = ctx[0];
		UUID prenda = ctx[1];

		// Vender 2 (stock 5 -> 3), total 100.
		String res = mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"lineas\":[{\"prendaId\":\"" + prenda
								+ "\",\"cantidad\":2,\"precioUnitario\":50.00}]}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID ventaId = UUID.fromString(json.readTree(res).get("id").asText());

		// Devolver 1 de 2 (parcial): la venta queda PARCIALMENTE_DEVUELTA y reembolsa 50.
		mvc.perform(post("/api/v1/ventas/{id}/devolver", ventaId).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"lineas\":[{\"prendaId\":\"" + prenda + "\",\"cantidad\":1}]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("PARCIALMENTE_DEVUELTA"))
				.andExpect(jsonPath("$.montoReembolsado").value(50.00))
				.andExpect(jsonPath("$.lineas[0].cantidadDevuelta").value(1));
		// Solo se reingresó 1 unidad: 3 -> 4.
		org.assertj.core.api.Assertions.assertThat(disponiblesDe(prenda)).isEqualTo(4);

		// Devolver el resto (sin cuerpo = todo lo pendiente): queda DEVUELTA y reembolsa el total.
		mvc.perform(post("/api/v1/ventas/{id}/devolver", ventaId).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("DEVUELTA"))
				.andExpect(jsonPath("$.montoReembolsado").value(100.00));
		org.assertj.core.api.Assertions.assertThat(disponiblesDe(prenda)).isEqualTo(5);
	}

	@Test
	void con_reembolsos_desactivados_la_venta_no_se_puede_devolver() throws Exception {
		UUID[] ctx = montar();
		UUID sucursal = ctx[0];
		UUID prenda = ctx[1];

		// Vender 2 (stock 5 -> 3).
		String res = mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"lineas\":[{\"prendaId\":\"" + prenda
								+ "\",\"cantidad\":2,\"precioUnitario\":50.00}]}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID ventaId = UUID.fromString(json.readTree(res).get("id").asText());

		// Política del local: reembolsos desactivados (RF-4.5).
		mvc.perform(put("/api/v1/configuracion").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"conteoStock\":true,\"multasActivo\":true,\"multiSucursal\":false,\"pagoEnLinea\":false,"
								+ "\"reembolsosActivos\":false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reembolsosActivos").value(false));

		// Devolver la venta se rechaza (409) y no reingresa stock (sigue en 3).
		mvc.perform(post("/api/v1/ventas/{id}/devolver", ventaId).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isConflict());
		org.assertj.core.api.Assertions.assertThat(disponiblesDe(prenda)).isEqualTo(3);
	}

	@Test
	void con_conteo_de_stock_apagado_la_venta_no_controla_inventario() throws Exception {
		UUID[] ctx = montar();
		UUID sucursal = ctx[0];
		UUID prenda = ctx[1];

		// Apagar el conteo de stock (RF-12.4).
		mvc.perform(put("/api/v1/configuracion").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"conteoStock\":false,\"multasActivo\":true,\"multiSucursal\":false,\"pagoEnLinea\":false}"))
				.andExpect(status().isOk());

		// Vender 9 con solo 5 en stock: ahora se permite (no controla inventario).
		mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"lineas\":[{\"prendaId\":\"" + prenda
								+ "\",\"cantidad\":9,\"precioUnitario\":50.00}]}"))
				.andExpect(status().isCreated());

		// El stock quedó intacto (no se descontó).
		org.assertj.core.api.Assertions.assertThat(disponiblesDe(prenda)).isEqualTo(5);
	}

	@Test
	void vender_mas_de_lo_disponible_devuelve_409() throws Exception {
		UUID[] ctx = montar();

		mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + ctx[0] + "\",\"lineas\":[{\"prendaId\":\"" + ctx[1]
								+ "\",\"cantidad\":9,\"precioUnitario\":50.00}]}"))
				.andExpect(status().isConflict());
	}

	@Test
	void vender_una_prenda_inexistente_devuelve_400() throws Exception {
		UUID[] ctx = montar();

		mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + ctx[0] + "\",\"lineas\":[{\"prendaId\":\"" + UUID.randomUUID()
								+ "\",\"cantidad\":1,\"precioUnitario\":50.00}]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void una_venta_sin_lineas_devuelve_400() throws Exception {
		UUID[] ctx = montar();

		mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + ctx[0] + "\",\"lineas\":[]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void la_clave_de_idempotencia_no_duplica_la_venta() throws Exception {
		UUID[] ctx = montar();
		UUID sucursal = ctx[0];
		UUID prenda = ctx[1];
		String body = "{\"sucursalId\":\"" + sucursal + "\",\"claveIdempotencia\":\"K-" + UUID.randomUUID()
				+ "\",\"lineas\":[{\"prendaId\":\"" + prenda + "\",\"cantidad\":1,\"precioUnitario\":50.00}]}";

		// El mismo POST dos veces (reintento/offline) no debe crear dos ventas (RF-17.6).
		mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
		mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());

		mvc.perform(get("/api/v1/ventas").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1)); // no se duplicó

		// Y el stock se descontó una sola vez (5 - 1 = 4), no dos.
		org.assertj.core.api.Assertions.assertThat(disponiblesDe(prenda)).isEqualTo(4);
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/ventas")).andExpect(status().isUnauthorized());
	}
}
