package com.costumi.backend.pagos;

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

/** Pagos (RF-6): registro ligado a un concepto e idempotencia por clave. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PagoIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private String dueno;

	private UUID sucursalDePrueba() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Pago " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		this.dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		String suc = mvc.perform(post("/api/v1/empresas/" + empresa + "/sucursales")
						.header("Authorization", "Bearer " + dueno).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Centro\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(suc).get("id").asText());
	}

	private UUID postId(String path, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	/** Crea una venta real de total = precioUnitario × cantidad (descuento 0) y devuelve su id. */
	private UUID ventaReal(UUID sucursal, int precioUnitario, int cantidad) throws Exception {
		UUID categoria = postId("/api/v1/categorias", "{\"nombre\":\"C " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"P\",\"tipoArticulo\":\"VENTA\",\"precioVenta\":" + precioUnitario + ".00}");
		postId("/api/v1/prendas/" + prenda + "/grupos-stock",
				"{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":50}");
		return postId("/api/v1/ventas", "{\"sucursalId\":\"" + sucursal + "\",\"lineas\":[{\"prendaId\":\""
				+ prenda + "\",\"cantidad\":" + cantidad + ",\"precioUnitario\":" + precioUnitario + ".00}]}");
	}

	@Test
	void registrar_un_pago_ligado_a_una_renta() throws Exception {
		UUID sucursal = sucursalDePrueba();
		UUID concepto = UUID.randomUUID();

		mvc.perform(post("/api/v1/pagos").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"RENTA\",\"conceptoId\":\""
								+ concepto + "\",\"monto\":40.00,\"metodo\":\"EFECTIVO\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.monto").value(40.00))
				.andExpect(jsonPath("$.metodo").value("EFECTIVO"));

		mvc.perform(get("/api/v1/pagos").param("conceptoId", concepto.toString())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void la_clave_de_idempotencia_evita_duplicar_el_cobro() throws Exception {
		UUID sucursal = sucursalDePrueba();
		UUID concepto = UUID.randomUUID();
		String cuerpo = "{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"VENTA\",\"conceptoId\":\""
				+ concepto + "\",\"monto\":25.00,\"metodo\":\"TARJETA\",\"claveIdempotencia\":\"K-" + concepto + "\"}";

		mvc.perform(post("/api/v1/pagos").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isCreated());
		mvc.perform(post("/api/v1/pagos").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isCreated());

		mvc.perform(get("/api/v1/pagos").param("conceptoId", concepto.toString())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1)); // no se duplicó
	}

	@Test
	void el_saldo_neto_resta_los_reembolsos_de_los_cobros() throws Exception {
		UUID sucursal = sucursalDePrueba();
		UUID concepto = UUID.randomUUID();

		// Cobro 100 (COBRO por defecto) y reembolso 30 -> saldo neto 70 (RF-6.9).
		mvc.perform(post("/api/v1/pagos").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"RENTA\",\"conceptoId\":\""
								+ concepto + "\",\"monto\":100.00,\"metodo\":\"EFECTIVO\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipoPago").value("COBRO"));

		mvc.perform(post("/api/v1/pagos").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"RENTA\",\"conceptoId\":\""
								+ concepto + "\",\"monto\":30.00,\"tipoPago\":\"REEMBOLSO\",\"metodo\":\"EFECTIVO\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipoPago").value("REEMBOLSO"));

		mvc.perform(get("/api/v1/pagos/saldo").param("conceptoId", concepto.toString())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.saldoNeto").value(70.00));
	}

	@Test
	void un_cobro_mixto_genera_un_pago_por_metodo_y_calcula_el_vuelto() throws Exception {
		UUID sucursal = sucursalDePrueba();
		UUID venta = ventaReal(sucursal, 50, 2); // total 100

		// 60 en efectivo (recibe 100 -> vuelto 40) + 40 con tarjeta = total 100 = saldo pendiente (RF-6.7).
		mvc.perform(post("/api/v1/pagos/mixto").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"VENTA\",\"conceptoId\":\""
								+ venta + "\",\"efectivoRecibido\":100.00,\"porciones\":["
								+ "{\"metodo\":\"EFECTIVO\",\"monto\":60.00},"
								+ "{\"metodo\":\"TARJETA\",\"monto\":40.00,\"referencia\":\"AUTH-9\"}]}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.total").value(100.00))
				.andExpect(jsonPath("$.vuelto").value(40.00))
				.andExpect(jsonPath("$.pagos.length()").value(2));

		// Quedaron los dos pagos ligados al concepto.
		mvc.perform(get("/api/v1/pagos").param("conceptoId", venta.toString())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void un_cobro_mixto_con_efectivo_insuficiente_devuelve_400() throws Exception {
		UUID sucursal = sucursalDePrueba();
		UUID venta = ventaReal(sucursal, 60, 1); // total 60

		// Recibe 50 en efectivo pero la parte en efectivo es 60 -> se rechaza (RF-6.7).
		mvc.perform(post("/api/v1/pagos/mixto").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"VENTA\",\"conceptoId\":\""
								+ venta + "\",\"efectivoRecibido\":50.00,\"porciones\":["
								+ "{\"metodo\":\"EFECTIVO\",\"monto\":60.00}]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void un_cobro_mixto_debe_cuadrar_con_el_saldo_pendiente() throws Exception {
		UUID sucursal = sucursalDePrueba();
		UUID venta = ventaReal(sucursal, 50, 2); // total 100

		// Cobrar de menos (80 != 100) -> 400 (RF-6.1).
		mvc.perform(post("/api/v1/pagos/mixto").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"VENTA\",\"conceptoId\":\""
								+ venta + "\",\"porciones\":[{\"metodo\":\"EFECTIVO\",\"monto\":80.00}]}"))
				.andExpect(status().isBadRequest());

		// Cobrar de más (120 != 100) -> 400.
		mvc.perform(post("/api/v1/pagos/mixto").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"VENTA\",\"conceptoId\":\""
								+ venta + "\",\"porciones\":[{\"metodo\":\"EFECTIVO\",\"monto\":120.00}]}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void un_cobro_mixto_es_idempotente_por_clave() throws Exception {
		UUID sucursal = sucursalDePrueba();
		UUID venta = ventaReal(sucursal, 50, 1); // total 50
		String cuerpo = "{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"VENTA\",\"conceptoId\":\"" + venta
				+ "\",\"claveIdempotencia\":\"MX-" + venta + "\",\"porciones\":["
				+ "{\"metodo\":\"EFECTIVO\",\"monto\":30.00},{\"metodo\":\"TARJETA\",\"monto\":20.00}]}";

		mvc.perform(post("/api/v1/pagos/mixto").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isCreated());
		mvc.perform(post("/api/v1/pagos/mixto").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isCreated());

		// Aun con dos envíos, siguen siendo 2 pagos (no se duplicó, RF-17.6).
		mvc.perform(get("/api/v1/pagos").param("conceptoId", venta.toString())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void el_deposito_se_rastrea_como_retencion_aparte_y_no_como_ingreso() throws Exception {
		UUID sucursal = sucursalDePrueba();
		UUID concepto = UUID.randomUUID();

		// Se retiene la garantía (100) como movimiento aparte (RF-6.2/6.8).
		mvc.perform(post("/api/v1/pagos").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"RENTA\",\"conceptoId\":\""
								+ concepto + "\",\"monto\":100.00,\"tipoPago\":\"DEPOSITO\",\"metodo\":\"TARJETA\"}"))
				.andExpect(status().isCreated());

		// Se devuelve el remanente (70) al liquidar.
		mvc.perform(post("/api/v1/pagos").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"RENTA\",\"conceptoId\":\""
								+ concepto + "\",\"monto\":70.00,\"tipoPago\":\"DEVOLUCION_DEPOSITO\",\"metodo\":\"TARJETA\"}"))
				.andExpect(status().isCreated());

		// La garantía activa es 100 − 70 = 30.
		mvc.perform(get("/api/v1/pagos/deposito").param("conceptoId", concepto.toString())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.retenido").value(100.00))
				.andExpect(jsonPath("$.devuelto").value(70.00))
				.andExpect(jsonPath("$.activo").value(30.00));

		// El depósito NO cuenta como ingreso de la operación: el saldo neto sigue en 0 (RF-6.2).
		mvc.perform(get("/api/v1/pagos/saldo").param("conceptoId", concepto.toString())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.saldoNeto").value(0));
	}

	@Test
	void el_comprobante_agrega_los_pagos_y_los_totales_de_la_operacion() throws Exception {
		UUID sucursal = sucursalDePrueba();
		UUID concepto = UUID.randomUUID();

		// Cobro 100, reembolso 30, depósito 50 (RF-6.5: el recibo agrega todo).
		for (String cuerpo : new String[] {
				"{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"VENTA\",\"conceptoId\":\"" + concepto
						+ "\",\"monto\":100.00,\"metodo\":\"EFECTIVO\"}",
				"{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"VENTA\",\"conceptoId\":\"" + concepto
						+ "\",\"monto\":30.00,\"tipoPago\":\"REEMBOLSO\",\"metodo\":\"EFECTIVO\"}",
				"{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"VENTA\",\"conceptoId\":\"" + concepto
						+ "\",\"monto\":50.00,\"tipoPago\":\"DEPOSITO\",\"metodo\":\"TARJETA\"}" }) {
			mvc.perform(post("/api/v1/pagos").header("Authorization", "Bearer " + dueno)
					.contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isCreated());
		}

		mvc.perform(get("/api/v1/pagos/comprobante").param("conceptoId", concepto.toString())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.conceptoId").value(concepto.toString()))
				.andExpect(jsonPath("$.pagos.length()").value(3))
				.andExpect(jsonPath("$.totalCobrado").value(100.00))
				.andExpect(jsonPath("$.totalReembolsado").value(30.00))
				.andExpect(jsonPath("$.saldoNeto").value(70.00)) // 100 − 30; el depósito no es ingreso
				.andExpect(jsonPath("$.deposito.activo").value(50.00));
	}

	@Test
	void el_comprobante_desglosa_el_impuesto_incluido_segun_la_tasa() throws Exception {
		UUID sucursal = sucursalDePrueba();
		UUID concepto = UUID.randomUUID();

		// Tasa 19% impuesto-incluido (RF-6.5/12.2).
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/configuracion")
						.header("Authorization", "Bearer " + dueno).contentType(MediaType.APPLICATION_JSON)
						.content("{\"conteoStock\":true,\"multasActivo\":true,\"multiSucursal\":false,"
								+ "\"pagoEnLinea\":false,\"tasaImpuesto\":0.19}"))
				.andExpect(status().isOk());

		// Cobro de 119 (ya incluye el impuesto).
		mvc.perform(post("/api/v1/pagos").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"RENTA\",\"conceptoId\":\""
								+ concepto + "\",\"monto\":119.00,\"metodo\":\"EFECTIVO\"}"))
				.andExpect(status().isCreated());

		// 119 impuesto-incluido al 19% -> base 100, impuesto 19.
		mvc.perform(get("/api/v1/pagos/comprobante").param("conceptoId", concepto.toString())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalCobrado").value(119.00))
				.andExpect(jsonPath("$.tasaImpuesto").value(0.19))
				.andExpect(jsonPath("$.baseImponible").value(100.00))
				.andExpect(jsonPath("$.impuesto").value(19.00));
	}

	@Test
	void el_pago_en_linea_rechaza_un_monto_que_no_cubre_el_total_pendiente() throws Exception {
		UUID sucursal = sucursalDePrueba();
		// Habilitar pago en línea en la empresa.
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/configuracion")
						.header("Authorization", "Bearer " + dueno).contentType(MediaType.APPLICATION_JSON)
						.content("{\"conteoStock\":true,\"multasActivo\":true,\"multiSucursal\":false,"
								+ "\"pagoEnLinea\":true,\"tasaImpuesto\":0}"))
				.andExpect(status().isOk());
		// Venta real de total 200 (100 × 2).
		UUID venta = ventaReal(sucursal, 100, 2);

		// Con tarjeta se paga TODO de golpe: un intento por menos del total pendiente (150 de 200) -> 400.
		mvc.perform(post("/api/v1/pagos/intento").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"VENTA\",\"conceptoId\":\""
								+ venta + "\",\"monto\":150.00,\"moneda\":\"COP\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/pagos").param("conceptoId", UUID.randomUUID().toString()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void webhook_sin_secreto_de_firma_configurado_devuelve_503() throws Exception {
		// SEC-5, fail-closed: sin `costumi.pasarela.mp.webhook-secret` (vacío en pruebas), el webhook rechaza
		// TODA confirmación (aunque traiga firma) -> no se puede confirmar un pago sin webhook firmado.
		mvc.perform(post("/api/v1/pagos/webhook").header("X-Signature", "loquesea")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"intentoId\":\"" + UUID.randomUUID() + "\",\"idPagoExterno\":\"mp-1\"}"))
				.andExpect(status().isServiceUnavailable());
	}
}
