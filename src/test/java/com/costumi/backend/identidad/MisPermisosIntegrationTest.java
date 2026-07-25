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
 * Los permisos del propio usuario (B-permisos): cualquier rol ve los suyos (por el token), y reflejan los
 * bloqueos que le puso el dueño. La app arma la nav a partir de esto, no del rol.
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
	void cada_usuario_ve_sus_permisos_y_reflejan_los_bloqueos() throws Exception {
		UUID empresa = crearEmpresaAprobada();
		AuthTestHelper.Sesion dueno = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		AuthTestHelper.Sesion mostrador = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa,
				Rol.MOSTRADOR);

		// El MOSTRADOR ve los suyos (200, no 403): tiene VENTAS/ACCION, NO tiene REPORTES/ACCION.
		mvc.perform(get("/api/v1/empleados/me/permisos").header("Authorization", "Bearer " + mostrador.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.seccion == 'VENTAS' && @.accion == 'ACCION')]").exists())
				.andExpect(jsonPath("$[?(@.seccion == 'REPORTES' && @.accion == 'ACCION')]").doesNotExist());

		// El DUEÑO tiene todo (incluye REPORTES/ACCION).
		mvc.perform(get("/api/v1/empleados/me/permisos").header("Authorization", "Bearer " + dueno.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.seccion == 'REPORTES' && @.accion == 'ACCION')]").exists());

		// El dueño le BLOQUEA VENTAS/ACCION al mostrador.
		mvc.perform(put("/api/v1/empleados/{id}/permisos", mostrador.usuarioId())
						.header("Authorization", "Bearer " + dueno.token()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"seccion\":\"VENTAS\",\"accion\":\"ACCION\",\"concedido\":false}"))
				.andExpect(status().isOk());

		// Ahora el mostrador ya NO tiene VENTAS/ACCION en sus permisos (pero sí conserva VENTAS/VER).
		mvc.perform(get("/api/v1/empleados/me/permisos").header("Authorization", "Bearer " + mostrador.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.seccion == 'VENTAS' && @.accion == 'ACCION')]").doesNotExist())
				.andExpect(jsonPath("$[?(@.seccion == 'VENTAS' && @.accion == 'VER')]").exists());
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
