package com.costumi.backend.pagos;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.identidad.AuthTestHelper;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.SucursalRepository;
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

/** Solicitud de reembolso (RF-4.5/6.9): solicitar, bandeja, rechazar y validaciones, por HTTP. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReembolsoIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	SucursalRepository sucursales;

	@Autowired
	PasswordEncoder passwordEncoder;

	private UUID crearEmpresa(String nombre) throws Exception {
		String body = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	private String token(UUID empresaId, Rol rol) throws Exception {
		return AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresaId, rol);
	}

	/** Registra un cobro (COBRO efectivo) por HTTP para que el concepto tenga saldo reembolsable. */
	private UUID cobrar(UUID empresa, String tk, String monto) throws Exception {
		UUID sucursal = AuthTestHelper.sucursal(sucursales, empresa);
		UUID venta = UUID.randomUUID();
		mvc.perform(post("/api/v1/pagos").header("Authorization", "Bearer " + tk)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"tipoConcepto\":\"VENTA\",\"conceptoId\":\""
								+ venta + "\",\"monto\":" + monto + ",\"metodo\":\"EFECTIVO\"}"))
				.andExpect(status().isCreated());
		return venta;
	}

	private String solicitarBody(UUID venta, String monto) {
		return "{\"tipoConcepto\":\"VENTA\",\"conceptoId\":\"" + venta + "\",\"monto\":" + monto
				+ ",\"motivo\":\"el cliente no quedó conforme\"}";
	}

	@Test
	void solicitar_aparece_en_la_bandeja_y_se_puede_rechazar_con_motivo() throws Exception {
		UUID empresa = crearEmpresa("Reemb " + UUID.randomUUID());
		String dueno = token(empresa, Rol.DUENO);
		UUID venta = cobrar(empresa, dueno, "100.00");

		String body = mvc.perform(post("/api/v1/reembolsos").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content(solicitarBody(venta, "80.00")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("PENDIENTE"))
				.andReturn().getResponse().getContentAsString();
		UUID solicitud = UUID.fromString(json.readTree(body).get("id").asText());

		mvc.perform(get("/api/v1/reembolsos").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.id == '" + solicitud + "')]").exists());

		mvc.perform(post("/api/v1/reembolsos/{id}/rechazar", solicitud).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"fuera del plazo de cambios\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("RECHAZADA"))
				.andExpect(jsonPath("$.motivoDecision").value("fuera del plazo de cambios"));
	}

	@Test
	void solicitar_por_mas_del_saldo_devuelve_409() throws Exception {
		UUID empresa = crearEmpresa("Reemb Saldo " + UUID.randomUUID());
		String dueno = token(empresa, Rol.DUENO);
		UUID venta = cobrar(empresa, dueno, "50.00");

		mvc.perform(post("/api/v1/reembolsos").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content(solicitarBody(venta, "80.00")))
				.andExpect(status().isConflict());
	}

	@Test
	void aprobar_sin_devolucion_registrada_devuelve_409() throws Exception {
		UUID empresa = crearEmpresa("Reemb Item " + UUID.randomUUID());
		String dueno = token(empresa, Rol.DUENO);
		UUID venta = cobrar(empresa, dueno, "100.00");
		String body = mvc.perform(post("/api/v1/reembolsos").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content(solicitarBody(venta, "80.00")))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID solicitud = UUID.fromString(json.readTree(body).get("id").asText());

		// La venta (id inventado) no está devuelta → no se puede aprobar.
		mvc.perform(post("/api/v1/reembolsos/{id}/aprobar", solicitud).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"ok\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void un_rol_sin_permiso_no_puede_solicitar_403() throws Exception {
		UUID empresa = crearEmpresa("Reemb Rol " + UUID.randomUUID());
		String bodega = token(empresa, Rol.BODEGA);

		mvc.perform(post("/api/v1/reembolsos").header("Authorization", "Bearer " + bodega)
						.contentType(MediaType.APPLICATION_JSON).content(solicitarBody(UUID.randomUUID(), "10.00")))
				.andExpect(status().isForbidden());
	}

	@Test
	void un_cliente_no_puede_pedir_el_reembolso_de_una_operacion_ajena_403() throws Exception {
		UUID empresa = crearEmpresa("Reemb Cli " + UUID.randomUUID());
		String cliente = token(null, Rol.CLIENTE); // cliente a nivel plataforma, sin ficha ni venta en esta empresa

		mvc.perform(post("/api/v1/reembolsos/cliente").header("Authorization", "Bearer " + cliente)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"empresaId\":\"" + empresa + "\",\"tipoConcepto\":\"VENTA\",\"conceptoId\":\""
								+ UUID.randomUUID() + "\",\"monto\":10.00,\"motivo\":\"quiero mi plata\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void el_personal_no_usa_el_endpoint_self_service_del_cliente_403() throws Exception {
		UUID empresa = crearEmpresa("Reemb Self " + UUID.randomUUID());
		String dueno = token(empresa, Rol.DUENO); // el endpoint /cliente es solo para rol CLIENTE

		mvc.perform(post("/api/v1/reembolsos/cliente").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"empresaId\":\"" + empresa + "\",\"tipoConcepto\":\"VENTA\",\"conceptoId\":\""
								+ UUID.randomUUID() + "\",\"monto\":10.00,\"motivo\":\"x\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/reembolsos")).andExpect(status().isUnauthorized());
	}
}
