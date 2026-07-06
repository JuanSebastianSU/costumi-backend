package com.costumi.backend.caja;

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

/** Caja (RF-6.3/6.10): apertura, movimientos, corte por método y cuadre de efectivo. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CajaIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private String dueno;
	private UUID sucursal;

	private void montar() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Caja " + UUID.randomUUID() + "\"}"))
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
		this.sucursal = UUID.fromString(json.readTree(suc).get("id").asText());
	}

	private UUID abrirTurno(String fondo) throws Exception {
		String res = mvc.perform(post("/api/v1/caja/turnos").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"fondoInicial\":" + fondo + "}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("ABIERTO"))
				.andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	private void movimiento(UUID turno, String tipo, String concepto, String monto, String metodo) throws Exception {
		mvc.perform(post("/api/v1/caja/turnos/{id}/movimientos", turno).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"tipo\":\"" + tipo + "\",\"concepto\":\"" + concepto + "\",\"monto\":" + monto
								+ ",\"metodo\":\"" + metodo + "\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void abrir_mover_y_cerrar_calcula_corte_y_cuadre() throws Exception {
		montar();
		UUID turno = abrirTurno("100.00");
		movimiento(turno, "INGRESO", "Venta", "50.00", "EFECTIVO");
		movimiento(turno, "EGRESO", "Gasto", "20.00", "EFECTIVO");
		movimiento(turno, "INGRESO", "Venta tarjeta", "80.00", "TARJETA");

		// Cierre: esperado efectivo 100+50-20 = 130 ; contado 125 -> diferencia -5.
		mvc.perform(post("/api/v1/caja/turnos/{id}/cerrar", turno).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"efectivoContado\":125.00}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("CERRADO"))
				.andExpect(jsonPath("$.corte.EFECTIVO").value(130.00))
				.andExpect(jsonPath("$.corte.TARJETA").value(80.00))
				.andExpect(jsonPath("$.diferenciaEfectivo").value(-5.00));
	}

	@Test
	void no_se_puede_mover_un_turno_cerrado_409() throws Exception {
		montar();
		UUID turno = abrirTurno("0.00");
		mvc.perform(post("/api/v1/caja/turnos/{id}/cerrar", turno).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"efectivoContado\":0.00}"))
				.andExpect(status().isOk());

		mvc.perform(post("/api/v1/caja/turnos/{id}/movimientos", turno).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"tipo\":\"INGRESO\",\"concepto\":\"X\",\"monto\":10.00,\"metodo\":\"EFECTIVO\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void un_turno_de_otra_empresa_no_se_encuentra_404() throws Exception {
		montar();
		String duenoA = this.dueno;
		UUID turnoDeA = abrirTurno("50.00");

		montar(); // nueva empresa B, nuevo dueño
		mvc.perform(post("/api/v1/caja/turnos/{id}/cerrar", turnoDeA).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"efectivoContado\":50.00}"))
				.andExpect(status().isNotFound());
		// silencia el warning de variable no usada
		org.assertj.core.api.Assertions.assertThat(duenoA).isNotBlank();
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/caja/turnos")).andExpect(status().isUnauthorized());
	}
}
