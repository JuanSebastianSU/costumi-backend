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

/** Matriz de capacidades por empleado (Fase B, paso 5): el dueño niega una capacidad y el empleado la pierde. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PermisosEmpleadoIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void el_dueno_puede_negar_una_capacidad_y_el_empleado_pierde_el_acceso() throws Exception {
		UUID empresa = crearEmpresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		AuthTestHelper.Sesion mostrador = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa,
				Rol.MOSTRADOR);

		// Por defecto (preset del rol) el Mostrador puede crear clientes.
		crearCliente(mostrador.token(), 201);

		// El dueño niega la capacidad CLIENTES_CREAR para ese empleado.
		establecer(dueno, mostrador.usuarioId(), "CLIENTES_CREAR", false).andExpect(status().isOk());

		// Ahora el Mostrador ya no puede crear clientes (403 del interceptor de permisos).
		crearCliente(mostrador.token(), 403);

		// La matriz refleja la capacidad negada.
		mvc.perform(get("/api/v1/empleados/{id}/permisos", mostrador.usuarioId())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.capacidad == 'CLIENTES_CREAR' && @.concedido == false)]").exists());

		// El dueño la vuelve a conceder y el acceso vuelve.
		establecer(dueno, mostrador.usuarioId(), "CLIENTES_CREAR", true).andExpect(status().isOk());
		crearCliente(mostrador.token(), 201);
	}

	@Test
	void un_encargado_no_puede_conceder_lo_que_no_tiene() throws Exception {
		UUID empresa = crearEmpresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		AuthTestHelper.Sesion encargado = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa,
				Rol.ENCARGADO);
		AuthTestHelper.Sesion mostrador = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa,
				Rol.MOSTRADOR);

		// El dueño le quita al encargado la capacidad de aprobar reembolsos.
		establecer(dueno, encargado.usuarioId(), "REEMBOLSOS_APROBAR", false).andExpect(status().isOk());

		// El encargado (que ya no la tiene) NO puede concedérsela a un mostrador -> 403.
		establecer(encargado.token(), mostrador.usuarioId(), "REEMBOLSOS_APROBAR", true)
				.andExpect(status().isForbidden());
	}

	@Test
	void un_encargado_no_puede_re_concederse_lo_que_el_dueno_le_quito() throws Exception {
		UUID empresa = crearEmpresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		AuthTestHelper.Sesion encargado = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa,
				Rol.ENCARGADO);

		establecer(dueno, encargado.usuarioId(), "PAGOS_REGISTRAR", false).andExpect(status().isOk());

		// B3: el encargado NO puede editar sus propios permisos (re-concederse PAGOS) -> 403 (pirámide).
		establecer(encargado.token(), encargado.usuarioId(), "PAGOS_REGISTRAR", true)
				.andExpect(status().isForbidden());
	}

	@Test
	void un_encargado_no_puede_editar_los_permisos_de_otro_encargado() throws Exception {
		UUID empresa = crearEmpresaAprobada();
		String encargadoA = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.ENCARGADO);
		AuthTestHelper.Sesion encargadoB = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa,
				Rol.ENCARGADO);
		// B3: no se gestiona a un igual en la pirámide -> 403.
		establecer(encargadoA, encargadoB.usuarioId(), "PAGOS_REGISTRAR", false).andExpect(status().isForbidden());
	}

	@Test
	void un_encargado_no_puede_invitar_un_dueno_pero_si_un_operativo() throws Exception {
		UUID empresa = crearEmpresaAprobada();
		String encargado = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.ENCARGADO);
		// B3: el encargado solo invita roles por debajo suyo; un DUEÑO no -> 403.
		mvc.perform(post("/api/v1/empleados").header("Authorization", "Bearer " + encargado)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"nuevo-" + UUID.randomUUID() + "@x.test\",\"rol\":\"DUENO\"}"))
				.andExpect(status().isForbidden());
		// Positivo: sí puede invitar un operativo.
		mvc.perform(post("/api/v1/empleados").header("Authorization", "Bearer " + encargado)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"mos-" + UUID.randomUUID() + "@x.test\",\"rol\":\"MOSTRADOR\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	void el_empleado_puede_ligarse_a_varias_sucursales() throws Exception {
		UUID empresa = crearEmpresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		AuthTestHelper.Sesion mostrador = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa,
				Rol.MOSTRADOR);
		mvc.perform(put("/api/v1/configuracion").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"conteoStock\":true,\"multasActivo\":true,\"multiSucursal\":true,\"pagoEnLinea\":false}"))
				.andExpect(status().isOk());
		UUID sucA = crearSucursal(dueno, empresa, "A");
		UUID sucB = crearSucursal(dueno, empresa, "B");

		mvc.perform(put("/api/v1/empleados/{id}/sucursales", mostrador.usuarioId())
						.header("Authorization", "Bearer " + dueno).contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalIds\":[\"" + sucA + "\",\"" + sucB + "\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void la_actividad_de_un_empleado_sin_ventas_es_cero() throws Exception {
		UUID empresa = crearEmpresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		AuthTestHelper.Sesion mostrador = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa,
				Rol.MOSTRADOR);
		mvc.perform(get("/api/v1/empleados/{id}/actividad", mostrador.usuarioId())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ventas").value(0))
				.andExpect(jsonPath("$.totalVendido").value(0));
	}

	// --- helpers ---

	private org.springframework.test.web.servlet.ResultActions establecer(String token, UUID usuarioId,
			String capacidad, boolean concedido) throws Exception {
		return mvc.perform(put("/api/v1/empleados/{id}/permisos", usuarioId)
				.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
				.content("{\"capacidad\":\"" + capacidad + "\",\"concedido\":" + concedido + "}"));
	}

	private void crearCliente(String token, int esperado) throws Exception {
		mvc.perform(post("/api/v1/clientes").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Cliente\"}"))
				.andExpect(status().is(esperado));
	}

	private UUID crearSucursal(String dueno, UUID empresa, String nombre) throws Exception {
		String res = mvc.perform(post("/api/v1/empresas/" + empresa + "/sucursales")
						.header("Authorization", "Bearer " + dueno).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	private UUID crearEmpresaAprobada() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Permisos " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		return empresa;
	}
}
