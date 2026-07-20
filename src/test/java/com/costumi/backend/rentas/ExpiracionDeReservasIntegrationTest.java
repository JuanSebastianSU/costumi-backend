package com.costumi.backend.rentas;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.identidad.AuthTestHelper;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import com.costumi.backend.rentas.aplicacion.ExpirarReservas;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Expiración de reservas (RF-3.5): con la ventana en 0 h, una reserva RESERVADA sin pagar se cancela; una
 * pagada por su importe se respeta.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "costumi.rentas.expiracion-reserva-horas=0")
class ExpiracionDeReservasIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	ExpirarReservas expirarReservas;

	private String dueno;

	private UUID postId(String path, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	@Test
	void expira_la_reserva_sin_pagar_y_respeta_la_pagada() throws Exception {
		UUID empresa = UUID.fromString(json.readTree(mvc.perform(post("/api/v1/empresas")
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Exp " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		this.dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", "{\"nombre\":\"Centro\"}");
		UUID cliente = postId("/api/v1/clientes", "{\"nombre\":\"Cliente\"}");
		UUID categoria = postId("/api/v1/categorias", "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Traje\",\"tipoArticulo\":\"RENTA\",\"precioRenta\":20.00}");
		postId("/api/v1/prendas/" + prenda + "/grupos-stock",
				"{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":5}");
		String rentaBody = "{\"sucursalId\":\"" + sucursal + "\",\"clienteId\":\"" + cliente + "\",\"prendaId\":\""
				+ prenda + "\",\"fechaRetiro\":\"2026-08-01\",\"fechaDevolucion\":\"2026-08-04\","
				+ "\"precioPorDia\":20.00,\"deposito\":50.00}"; // importe = 20 × 3 días = 60

		UUID sinPagar = postId("/api/v1/rentas", rentaBody);
		UUID pagada = postId("/api/v1/rentas", rentaBody);
		// Pagar la segunda por su importe (60) -> queda cubierta.
		mvc.perform(post("/api/v1/pagos").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"RENTA\",\"conceptoId\":\""
								+ pagada + "\",\"monto\":60.00,\"metodo\":\"EFECTIVO\"}"))
				.andExpect(status().isCreated());

		int canceladas = expirarReservas.ejecutar();
		assertThat(canceladas).isGreaterThanOrEqualTo(1);

		// La sin pagar quedó CANCELADA; la pagada sigue RESERVADA.
		mvc.perform(get("/api/v1/rentas").param("clienteId", cliente.toString())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + sinPagar + "')].estado")
						.value(org.hamcrest.Matchers.hasItem("CANCELADA")))
				.andExpect(jsonPath("$.contenido[?(@.id == '" + pagada + "')].estado")
						.value(org.hamcrest.Matchers.hasItem("RESERVADA")));
	}
}
