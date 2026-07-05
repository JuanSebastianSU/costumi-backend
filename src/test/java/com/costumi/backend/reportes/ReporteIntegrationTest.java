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
