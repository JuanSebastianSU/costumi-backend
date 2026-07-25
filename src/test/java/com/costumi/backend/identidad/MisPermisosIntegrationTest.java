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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Las capacidades del propio usuario (Fase B, paso 5): cualquier rol ve las suyas (por el token) y reflejan
 * los bloqueos que le puso el dueño. La app arma la navegación a partir de esto, no del rol.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MisPermisosIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void cada_usuario_ve_sus_capacidades_y_reflejan_los_bloqueos() throws Exception {
		UUID empresa = crearEmpresaAprobada();
		AuthTestHelper.Sesion dueno = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		AuthTestHelper.Sesion mostrador = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa,
				Rol.MOSTRADOR);

		// El MOSTRADOR ve las suyas (200, no 403): tiene VENTAS_REGISTRAR, NO tiene REPORTES_VER.
		mvc.perform(get("/api/v1/empleados/me/permisos").header("Authorization", "Bearer " + mostrador.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.capacidad == 'VENTAS_REGISTRAR')]").exists())
				.andExpect(jsonPath("$[?(@.capacidad == 'REPORTES_VER')]").doesNotExist());

		// El DUEÑO tiene todo (incluye REPORTES_VER).
		mvc.perform(get("/api/v1/empleados/me/permisos").header("Authorization", "Bearer " + dueno.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.capacidad == 'REPORTES_VER')]").exists());

		// El dueño le BLOQUEA VENTAS_REGISTRAR al mostrador.
		mvc.perform(put("/api/v1/empleados/{id}/permisos", mostrador.usuarioId())
						.header("Authorization", "Bearer " + dueno.token()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"capacidad\":\"VENTAS_REGISTRAR\",\"concedido\":false}"))
				.andExpect(status().isOk());

		// Ahora el mostrador ya NO tiene VENTAS_REGISTRAR (pero sí conserva VENTAS_VER).
		mvc.perform(get("/api/v1/empleados/me/permisos").header("Authorization", "Bearer " + mostrador.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.capacidad == 'VENTAS_REGISTRAR')]").doesNotExist())
				.andExpect(jsonPath("$[?(@.capacidad == 'VENTAS_VER')]").exists());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/empleados/me/permisos")).andExpect(status().isUnauthorized());
	}

	private UUID crearEmpresaAprobada() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Perm " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		return empresa;
	}
}
