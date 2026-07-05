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
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/pagos").param("conceptoId", UUID.randomUUID().toString()))
				.andExpect(status().isUnauthorized());
	}
}
