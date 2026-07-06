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

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
