package com.costumi.backend.identidad;

import com.costumi.backend.TestcontainersConfiguration;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reasignación piramidal de sucursales (Fase B, paso 4): el encargado solo puede asignar empleados a SUS
 * propias sucursales; el dueño, a cualquiera.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AsignacionSucursalPiramideIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void el_encargado_solo_asigna_a_sus_propias_sucursales() throws Exception {
		UUID empresa = empresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		// Habilita multi-sucursal para poder tener dos.
		mvc.perform(put("/api/v1/configuracion").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"conteoStock\":false,\"multasActivo\":true,\"multiSucursal\":true,\"pagoEnLinea\":false}"))
				.andExpect(status().isOk());
		UUID sucA = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"A\"}");
		UUID sucB = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"B\"}");

		String correoEnc = "enc-" + UUID.randomUUID() + "@costumi.test";
		UUID encargadoId = crearEmpleadoActivo(dueno, correoEnc, "ENCARGADO");
		// El dueño asigna al encargado SOLO la sucursal A.
		mvc.perform(put("/api/v1/empleados/{id}/sucursales", encargadoId).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"sucursalIds\":[\"" + sucA + "\"]}"))
				.andExpect(status().isOk());
		String encargado = loginToken(correoEnc, "secret123");

		UUID mostradorId = crearEmpleadoActivo(dueno, "mos-" + UUID.randomUUID() + "@costumi.test", "MOSTRADOR");

		// El encargado NO puede asignar el mostrador a la sucursal B (no es suya) → 403.
		mvc.perform(put("/api/v1/empleados/{id}/sucursales", mostradorId).header("Authorization", "Bearer " + encargado)
						.contentType(MediaType.APPLICATION_JSON).content("{\"sucursalIds\":[\"" + sucB + "\"]}"))
				.andExpect(status().isForbidden());

		// Pero sí a la sucursal A (que es suya) → 200.
		mvc.perform(put("/api/v1/empleados/{id}/sucursales", mostradorId).header("Authorization", "Bearer " + encargado)
						.contentType(MediaType.APPLICATION_JSON).content("{\"sucursalIds\":[\"" + sucA + "\"]}"))
				.andExpect(status().isOk());
	}

	// --- helpers ---

	private UUID crearEmpleadoActivo(String dueno, String correo, String rol) throws Exception {
		String res = mvc.perform(post("/api/v1/empleados").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"rol\":\"" + rol + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String enlace = json.readTree(res).get("enlace").asText();
		mvc.perform(post("/api/v1/invitaciones/aceptar").contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"" + enlace + "\",\"password\":\"secret123\",\"aceptaTerminos\":true}"))
				.andExpect(status().isOk());
		return usuarios.buscarPorEmail(correo).orElseThrow().id();
	}

	private String loginToken(String correo, String password) throws Exception {
		String res = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return json.readTree(res).get("accessToken").asText();
	}

	private UUID empresaAprobada() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Pir " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		return empresa;
	}

	private UUID postId(String path, String token, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}
}
