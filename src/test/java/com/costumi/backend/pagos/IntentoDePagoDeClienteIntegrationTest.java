package com.costumi.backend.pagos;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.identidad.AuthTestHelper;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import com.costumi.backend.pagos.dominio.PasarelaDePago;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pago en línea iniciado por el propio CLIENTE del marketplace (RF-6.11/14.4): paga su propia venta de golpe
 * y obtiene la URL de checkout; no puede pagar la operación de otro (403) y el personal no usa este endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, IntentoDePagoDeClienteIntegrationTest.PasarelaStubConfig.class})
class IntentoDePagoDeClienteIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	/** Stub de la pasarela: evita depender de credenciales reales; devuelve una URL de checkout falsa. */
	@TestConfiguration
	static class PasarelaStubConfig {
		@Bean
		@Primary
		PasarelaDePago pasarelaStub() {
			return new PasarelaDePago() {
				@Override
				public boolean configurada() {
					return true;
				}

				@Override
				public ResultadoCheckout crearCheckout(BigDecimal monto, String moneda, String referencia,
						String descripcion) {
					return new ResultadoCheckout("https://checkout.test/" + referencia, "ext-" + referencia);
				}

				@Override
				public EstadoPagoExterno consultarPago(String idPagoExterno) {
					return new EstadoPagoExterno(true, BigDecimal.ZERO);
				}

				@Override
				public void reembolsar(String idPagoExterno, BigDecimal monto) {
				}
			};
		}
	}

	private record Tienda(UUID empresa, String dueno, UUID sucursal, UUID prenda) {
	}

	private UUID postId(String path, String token, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	/** Tienda aprobada, con sucursal, una prenda vendible (precioVenta 90) con stock y el pago en línea activo. */
	private Tienda montarTiendaConPagoEnLinea() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"PagoCli " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());

		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());

		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"Centro\"}");
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Camisa\",\"tipoArticulo\":\"AMBOS\",\"precioRenta\":30.00,\"precioVenta\":90.00}");
		mvc.perform(post("/api/v1/prendas/{id}/grupos-stock", prenda).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":5}"))
				.andExpect(status().isCreated());
		// Activar el pago en línea de la tienda (si no, el intento responde 409).
		mvc.perform(put("/api/v1/configuracion").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"conteoStock\":true,\"multasActivo\":true,\"multiSucursal\":false,"
								+ "\"pagoEnLinea\":true,\"tasaImpuesto\":0}"))
				.andExpect(status().isOk());
		return new Tienda(empresa, dueno, sucursal, prenda);
	}

	/** El cliente compra una prenda a su nombre y devuelve el id de la venta (total 90 × cantidad). */
	private UUID clienteCompra(Tienda t, String clienteToken, int cantidad) throws Exception {
		mvc.perform(post("/api/v1/carritos/items").header("Authorization", "Bearer " + clienteToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + t.sucursal() + "\",\"empresaId\":\"" + t.empresa()
								+ "\",\"tipo\":\"VENTA\",\"prendaId\":\"" + t.prenda() + "\",\"cantidad\":" + cantidad + "}"))
				.andExpect(status().isOk());
		String res = mvc.perform(post("/api/v1/carritos/checkout").header("Authorization", "Bearer " + clienteToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + t.sucursal() + "\",\"empresaId\":\"" + t.empresa() + "\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("ventaId").asText());
	}

	private String intentoBody(Tienda t, UUID venta, String monto) {
		return "{\"empresaId\":\"" + t.empresa() + "\",\"sucursalId\":\"" + t.sucursal()
				+ "\",\"tipoConcepto\":\"VENTA\",\"conceptoId\":\"" + venta + "\",\"monto\":" + monto
				+ ",\"moneda\":\"COP\"}";
	}

	@Test
	void el_cliente_paga_en_linea_su_propia_venta_y_obtiene_la_url_de_checkout() throws Exception {
		Tienda t = montarTiendaConPagoEnLinea();
		String cliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);
		UUID venta = clienteCompra(t, cliente, 1); // total 90.00

		mvc.perform(post("/api/v1/pagos/intento/cliente").header("Authorization", "Bearer " + cliente)
						.contentType(MediaType.APPLICATION_JSON).content(intentoBody(t, venta, "90.00")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.intentoId").exists())
				.andExpect(jsonPath("$.urlCheckout").value(org.hamcrest.Matchers.startsWith("https://checkout.test/")));
	}

	@Test
	void con_tarjeta_se_paga_todo_de_golpe_un_monto_parcial_devuelve_400() throws Exception {
		Tienda t = montarTiendaConPagoEnLinea();
		String cliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);
		UUID venta = clienteCompra(t, cliente, 1); // total 90.00

		// La operación es suya (pasa la propiedad) pero 50 no cubre el total pendiente (90) -> 400.
		mvc.perform(post("/api/v1/pagos/intento/cliente").header("Authorization", "Bearer " + cliente)
						.contentType(MediaType.APPLICATION_JSON).content(intentoBody(t, venta, "50.00")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void un_cliente_no_puede_pagar_la_venta_de_otro_403() throws Exception {
		Tienda t = montarTiendaConPagoEnLinea();
		String comprador = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);
		UUID venta = clienteCompra(t, comprador, 1);

		// Otro cliente del marketplace, sin ficha ni operación en esta tienda, intenta pagar esa venta.
		String intruso = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);
		mvc.perform(post("/api/v1/pagos/intento/cliente").header("Authorization", "Bearer " + intruso)
						.contentType(MediaType.APPLICATION_JSON).content(intentoBody(t, venta, "90.00")))
				.andExpect(status().isForbidden());
	}

	@Test
	void el_personal_no_usa_el_endpoint_self_service_del_cliente_403() throws Exception {
		Tienda t = montarTiendaConPagoEnLinea();
		// El endpoint /intento/cliente es solo para rol CLIENTE (el personal usa /pagos/intento).
		mvc.perform(post("/api/v1/pagos/intento/cliente").header("Authorization", "Bearer " + t.dueno())
						.contentType(MediaType.APPLICATION_JSON).content(intentoBody(t, UUID.randomUUID(), "90.00")))
				.andExpect(status().isForbidden());
	}
}
