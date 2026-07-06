package com.costumi.backend.reportes;

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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Reportes (RF-9): resumen de ingresos por renta/venta, restringido y acotado al tenant. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReporteIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private UUID empresa;
	private UUID sucursal;

	private String tokenRol(Rol rol) throws Exception {
		return AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, rol);
	}

	private void montar() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Rep " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		this.empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		String dueno = tokenRol(Rol.DUENO);
		String suc = mvc.perform(post("/api/v1/empresas/" + empresa + "/sucursales")
						.header("Authorization", "Bearer " + dueno).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Centro\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		this.sucursal = UUID.fromString(json.readTree(suc).get("id").asText());
	}

	private void pago(String token, String tipo, String monto) throws Exception {
		mvc.perform(post("/api/v1/pagos").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"" + tipo + "\",\"conceptoId\":\""
								+ UUID.randomUUID() + "\",\"monto\":" + monto + ",\"metodo\":\"EFECTIVO\"}"))
				.andExpect(status().isCreated());
	}

	private void pago(String token, String tipo, String monto, String metodo, String tipoPago) throws Exception {
		mvc.perform(post("/api/v1/pagos").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"" + tipo + "\",\"conceptoId\":\""
								+ UUID.randomUUID() + "\",\"monto\":" + monto + ",\"metodo\":\"" + metodo
								+ "\",\"tipoPago\":\"" + tipoPago + "\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	void el_resumen_suma_los_ingresos_por_tipo() throws Exception {
		montar();
		String dueno = tokenRol(Rol.DUENO);
		pago(dueno, "RENTA", "40.00");
		pago(dueno, "VENTA", "60.00");

		mvc.perform(get("/api/v1/reportes/ingresos").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ingresosPorRenta").value(40.00))
				.andExpect(jsonPath("$.ingresosPorVenta").value(60.00))
				.andExpect(jsonPath("$.total").value(100.00));
	}

	private UUID postId(String path, String token, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	@Test
	void la_ganancia_es_ingreso_menos_costo_de_ventas() throws Exception {
		montar();
		String dueno = tokenRol(Rol.DUENO);
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Peluca " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria + "\",\"nombre\":\"Peluca\","
				+ "\"tipoArticulo\":\"VENTA\",\"precioVenta\":50.00,\"costoAdquisicion\":30.00}");
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno, "{\"combinacion\":[],\"cantidadInicial\":5}");

		// Vende 2 unidades (costo 2×30 = 60) y registra un pago de venta por 100 (ingreso).
		mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"lineas\":[{\"prendaId\":\"" + prenda
								+ "\",\"cantidad\":2,\"precioUnitario\":50.00}]}"))
				.andExpect(status().isCreated());
		pago(dueno, "VENTA", "100.00");

		mvc.perform(get("/api/v1/reportes/ganancia").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ingresos").value(100.00))
				.andExpect(jsonPath("$.costoDeVentas").value(60.00))
				.andExpect(jsonPath("$.ganancia").value(40.00));
	}

	@Test
	void reporta_rentas_vencidas_y_depositos_activos() throws Exception {
		montar();
		String dueno = tokenRol(Rol.DUENO);
		UUID cliente = postId("/api/v1/clientes", dueno, "{\"nombre\":\"Cliente\"}");
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Camisa " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Camisa\",\"tipoArticulo\":\"RENTA\",\"precioRenta\":20.00}");
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno, "{\"combinacion\":[],\"cantidadInicial\":2}");

		// Renta con fechas de 2020 (ya pasadas) y depósito 100; se entrega -> queda ACTIVA (y vencida).
		UUID renta = postId("/api/v1/rentas", dueno, "{\"sucursalId\":\"" + sucursal + "\",\"clienteId\":\"" + cliente
				+ "\",\"prendaId\":\"" + prenda + "\",\"fechaRetiro\":\"2020-01-01\",\"fechaDevolucion\":\"2020-01-05\","
				+ "\"precioPorDia\":20.00,\"deposito\":100.00}");
		mvc.perform(post("/api/v1/rentas/{id}/entregar", renta).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk());

		// RF-9.1: la renta ACTIVA con devolución en el pasado aparece como vencida, con días de atraso.
		mvc.perform(get("/api/v1/reportes/rentas-vencidas").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].rentaId").value(renta.toString()))
				.andExpect(jsonPath("$[0].diasVencida").value(greaterThan(0)))
				.andExpect(jsonPath("$[0].deposito").value(100.00));

		// RF-9.1: su depósito (100) cuenta como activo mientras la renta no se cierra.
		mvc.perform(get("/api/v1/reportes/depositos-activos").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(100.00));

		// Filtrar por otra sucursal no muestra esta renta.
		mvc.perform(get("/api/v1/reportes/rentas-vencidas").param("sucursalId", UUID.randomUUID().toString())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void reporta_ingresos_netos_por_metodo() throws Exception {
		montar();
		String dueno = tokenRol(Rol.DUENO);
		// Cobros: efectivo 100, tarjeta 50, transferencia 30. Reembolso efectivo 20. Depósito tarjeta 200 (no ingreso).
		pago(dueno, "VENTA", "100.00", "EFECTIVO", "COBRO");
		pago(dueno, "VENTA", "50.00", "TARJETA", "COBRO");
		pago(dueno, "RENTA", "30.00", "TRANSFERENCIA", "COBRO");
		pago(dueno, "VENTA", "20.00", "EFECTIVO", "REEMBOLSO");
		pago(dueno, "RENTA", "200.00", "TARJETA", "DEPOSITO");

		mvc.perform(get("/api/v1/reportes/ingresos-por-metodo").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.efectivo").value(80.00)) // 100 − 20 reembolso
				.andExpect(jsonPath("$.tarjeta").value(50.00)) // el depósito 200 NO es ingreso
				.andExpect(jsonPath("$.transferencia").value(30.00))
				.andExpect(jsonPath("$.total").value(160.00));

		// Rango en el futuro -> sin ingresos.
		mvc.perform(get("/api/v1/reportes/ingresos-por-metodo").param("desde", "2099-01-01")
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(0));
	}

	@Test
	void reporta_rankings_y_ventas_por_empleado() throws Exception {
		montar();
		String dueno = tokenRol(Rol.DUENO);
		UUID cliente = postId("/api/v1/clientes", dueno, "{\"nombre\":\"Cliente\"}");
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prendaV = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Peluca\",\"tipoArticulo\":\"VENTA\",\"precioVenta\":50.00}");
		postId("/api/v1/prendas/" + prendaV + "/grupos-stock", dueno, "{\"combinacion\":[],\"cantidadInicial\":10}");
		UUID prendaR = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Traje\",\"tipoArticulo\":\"RENTA\",\"precioRenta\":20.00}");
		postId("/api/v1/prendas/" + prendaR + "/grupos-stock", dueno, "{\"combinacion\":[],\"cantidadInicial\":5}");

		// Vende 3 de la prenda de venta.
		mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"lineas\":[{\"prendaId\":\"" + prendaV
								+ "\",\"cantidad\":3,\"precioUnitario\":50.00}]}"))
				.andExpect(status().isCreated());
		// Renta la prenda de renta.
		postId("/api/v1/rentas", dueno, "{\"sucursalId\":\"" + sucursal + "\",\"clienteId\":\"" + cliente
				+ "\",\"prendaId\":\"" + prendaR + "\",\"fechaRetiro\":\"2026-09-01\",\"fechaDevolucion\":\"2026-09-03\","
				+ "\"precioPorDia\":20.00,\"deposito\":100.00}");

		mvc.perform(get("/api/v1/reportes/mas-vendidos").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].prendaId").value(prendaV.toString()))
				.andExpect(jsonPath("$[0].unidades").value(3))
				.andExpect(jsonPath("$[0].monto").value(150.00));

		mvc.perform(get("/api/v1/reportes/mas-rentados").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].prendaId").value(prendaR.toString()))
				.andExpect(jsonPath("$[0].unidades").value(1));

		mvc.perform(get("/api/v1/reportes/ventas-por-empleado").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].numeroVentas").value(1))
				.andExpect(jsonPath("$[0].total").value(150.00));
	}

	@Test
	void reporta_el_tablero_y_el_resumen_de_inventario() throws Exception {
		montar();
		String dueno = tokenRol(Rol.DUENO);
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Peluca\",\"tipoArticulo\":\"VENTA\",\"precioVenta\":50.00}");
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno, "{\"combinacion\":[],\"cantidadInicial\":10}");

		// RF-9.3: tablero por grupo con el conteo por estado.
		mvc.perform(get("/api/v1/reportes/inventario/tablero").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].prendaId").value(prenda.toString()))
				.andExpect(jsonPath("$[0].disponibles").value(10))
				.andExpect(jsonPath("$[0].danadas").value(0));

		// RF-9.1: resumen con total, valor de inventario (10 × 50) y utilización (sin rentas activas = 0).
		mvc.perform(get("/api/v1/reportes/inventario/resumen").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalUnidades").value(10))
				.andExpect(jsonPath("$.disponibles").value(10))
				.andExpect(jsonPath("$.rentadasAhora").value(0))
				.andExpect(jsonPath("$.valorInventario").value(500.00));
	}

	@Test
	void desglosa_las_ventas_por_dimension_de_etiqueta() throws Exception {
		montar();
		String dueno = tokenRol(Rol.DUENO);
		// Dimensión "Color" (aplica a todas las categorías) con el valor "Rojo".
		UUID tipoColor = postId("/api/v1/tipos-etiqueta", dueno, "{\"nombre\":\"Color " + UUID.randomUUID()
				+ "\",\"defineVariante\":true,\"seleccionablePorCliente\":false,\"categoriasQueAplica\":[]}");
		UUID rojo = postId("/api/v1/tipos-etiqueta/" + tipoColor + "/valores", dueno, "{\"valor\":\"Rojo\"}");

		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Peluca\",\"tipoArticulo\":\"VENTA\",\"precioVenta\":50.00,\"etiquetas\":[{"
				+ "\"tipoEtiquetaId\":\"" + tipoColor + "\",\"valorEtiquetaId\":\"" + rojo + "\"}]}");
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno, "{\"combinacion\":[],\"cantidadInicial\":10}");

		mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"lineas\":[{\"prendaId\":\"" + prenda
								+ "\",\"cantidad\":2,\"precioUnitario\":50.00}]}"))
				.andExpect(status().isCreated());

		// RF-9.1: ventas desglosadas por la dimensión Color -> Rojo con 2 unidades y 100 de monto.
		mvc.perform(get("/api/v1/reportes/ventas-por-etiqueta").param("tipoEtiquetaId", tipoColor.toString())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].valor").value("Rojo"))
				.andExpect(jsonPath("$[0].unidades").value(2))
				.andExpect(jsonPath("$[0].monto").value(100.00));
	}

	@Test
	void exporta_el_tablero_de_inventario_en_csv() throws Exception {
		montar();
		String dueno = tokenRol(Rol.DUENO);
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Peluca\",\"tipoArticulo\":\"VENTA\",\"precioVenta\":50.00}");
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno, "{\"combinacion\":[],\"cantidadInicial\":10}");

		// RF-9.2: export a CSV, descargable (Content-Disposition adjunto).
		mvc.perform(get("/api/v1/reportes/export/inventario-tablero.csv").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/csv"))
				.andExpect(header().string("Content-Disposition", containsString("inventario-tablero.csv")))
				.andExpect(content().string(containsString("prendaId,prenda,disponibles")))
				.andExpect(content().string(containsString("Peluca")));
	}

	@Test
	void un_rol_sin_permiso_no_ve_reportes_403() throws Exception {
		montar();
		String mostrador = tokenRol(Rol.MOSTRADOR);

		mvc.perform(get("/api/v1/reportes/ingresos").header("Authorization", "Bearer " + mostrador))
				.andExpect(status().isForbidden());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/reportes/ingresos")).andExpect(status().isUnauthorized());
	}
}
