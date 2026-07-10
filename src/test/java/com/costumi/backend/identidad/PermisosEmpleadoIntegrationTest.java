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

/** Permisos granulares por empleado (RF-1.5): el dueño desactiva una casilla y el empleado pierde el acceso. */
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

	private void crearCliente(String token, int esperado) throws Exception {
		mvc.perform(post("/api/v1/clientes").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Cliente\"}"))
				.andExpect(status().is(esperado));
	}

	@Test
	void el_dueno_puede_revocar_una_casilla_y_el_empleado_pierde_el_acceso() throws Exception {
		UUID empresa = crearEmpresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		AuthTestHelper.Sesion mostrador = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa,
				Rol.MOSTRADOR);

		// Por defecto (plantilla del rol) el Mostrador puede crear clientes.
		crearCliente(mostrador.token(), 201);

		// El dueño desactiva la casilla CLIENTES/ACCION para ese empleado.
		mvc.perform(put("/api/v1/empleados/{id}/permisos", mostrador.usuarioId())
						.header("Authorization", "Bearer " + dueno).contentType(MediaType.APPLICATION_JSON)
						.content("{\"seccion\":\"CLIENTES\",\"accion\":\"ACCION\",\"concedido\":false}"))
				.andExpect(status().isOk());

		// Ahora el Mostrador ya no puede crear clientes (403 del interceptor de permisos).
		crearCliente(mostrador.token(), 403);

		// La matriz refleja la casilla desactivada.
		mvc.perform(get("/api/v1/empleados/{id}/permisos", mostrador.usuarioId())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.seccion == 'CLIENTES' && @.accion == 'ACCION' && @.concedido == false)]")
						.exists());

		// El dueño la reactiva y el acceso vuelve.
		mvc.perform(put("/api/v1/empleados/{id}/permisos", mostrador.usuarioId())
						.header("Authorization", "Bearer " + dueno).contentType(MediaType.APPLICATION_JSON)
						.content("{\"seccion\":\"CLIENTES\",\"accion\":\"ACCION\",\"concedido\":true}"))
				.andExpect(status().isOk());
		crearCliente(mostrador.token(), 201);
	}

	@Test
	void el_empleado_puede_ligarse_a_varias_sucursales() throws Exception {
		UUID empresa = crearEmpresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		AuthTestHelper.Sesion mostrador = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa,
				Rol.MOSTRADOR);
		// Habilita multi-sucursal (RF-12.4) para poder tener más de una sucursal.
		mvc.perform(put("/api/v1/configuracion").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"conteoStock\":true,\"multasActivo\":true,\"multiSucursal\":true,\"pagoEnLinea\":false}"))
				.andExpect(status().isOk());
		UUID sucA = crearSucursal(dueno, empresa, "A");
		UUID sucB = crearSucursal(dueno, empresa, "B");

		// RF-1.2: liga el empleado a dos sucursales.
		mvc.perform(put("/api/v1/empleados/{id}/sucursales", mostrador.usuarioId())
						.header("Authorization", "Bearer " + dueno).contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalIds\":[\"" + sucA + "\",\"" + sucB + "\"]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));

		mvc.perform(get("/api/v1/empleados/{id}/sucursales", mostrador.usuarioId())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void la_actividad_de_un_empleado_sin_ventas_es_cero() throws Exception {
		UUID empresa = crearEmpresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		AuthTestHelper.Sesion mostrador = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa,
				Rol.MOSTRADOR);

		// RF-8.2: sin ventas registradas, la actividad del empleado es cero.
		mvc.perform(get("/api/v1/empleados/{id}/actividad", mostrador.usuarioId())
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ventas").value(0))
				.andExpect(jsonPath("$.totalVendido").value(0));
	}

	@Test
	void un_encargado_no_puede_crear_un_dueno() throws Exception {
		UUID empresa = crearEmpresaAprobada();
		String encargado = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.ENCARGADO);
		// B3: el encargado solo crea roles por debajo suyo; un DUEÑO no se crea por alta -> 403.
		mvc.perform(post("/api/v1/empleados").header("Authorization", "Bearer " + encargado)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"nuevo-" + UUID.randomUUID() + "@x.test\",\"password\":\"secret123\","
								+ "\"rol\":\"DUENO\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void un_encargado_puede_crear_un_operativo() throws Exception {
		UUID empresa = crearEmpresaAprobada();
		String encargado = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.ENCARGADO);
		// Positivo: el encargado sí puede crear roles operativos (por debajo suyo).
		mvc.perform(post("/api/v1/empleados").header("Authorization", "Bearer " + encargado)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"mos-" + UUID.randomUUID() + "@x.test\",\"password\":\"secret123\","
								+ "\"rol\":\"MOSTRADOR\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	void un_encargado_no_puede_re_concederse_lo_que_el_dueno_le_quito() throws Exception {
		UUID empresa = crearEmpresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		AuthTestHelper.Sesion encargado = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa,
				Rol.ENCARGADO);

		// El dueño le quita PAGOS al encargado (el dueño sí puede: está por encima).
		mvc.perform(put("/api/v1/empleados/{id}/permisos", encargado.usuarioId())
						.header("Authorization", "Bearer " + dueno).contentType(MediaType.APPLICATION_JSON)
						.content("{\"seccion\":\"PAGOS\",\"accion\":\"ACCION\",\"concedido\":false}"))
				.andExpect(status().isOk());

		// B3: el encargado NO puede editar sus propios permisos (re-concederse PAGOS) -> 403.
		mvc.perform(put("/api/v1/empleados/{id}/permisos", encargado.usuarioId())
						.header("Authorization", "Bearer " + encargado.token()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"seccion\":\"PAGOS\",\"accion\":\"ACCION\",\"concedido\":true}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void un_encargado_no_puede_editar_los_permisos_de_otro_encargado() throws Exception {
		UUID empresa = crearEmpresaAprobada();
		String encargadoA = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.ENCARGADO);
		AuthTestHelper.Sesion encargadoB = AuthTestHelper.sesion(mvc, json, usuarios, passwordEncoder, empresa,
				Rol.ENCARGADO);
		// B3: no se gestiona a un igual en la pirámide -> 403.
		mvc.perform(put("/api/v1/empleados/{id}/permisos", encargadoB.usuarioId())
						.header("Authorization", "Bearer " + encargadoA).contentType(MediaType.APPLICATION_JSON)
						.content("{\"seccion\":\"PAGOS\",\"accion\":\"ACCION\",\"concedido\":false}"))
				.andExpect(status().isForbidden());
	}

	private UUID crearSucursal(String dueno, UUID empresa, String nombre) throws Exception {
		String res = mvc.perform(post("/api/v1/empresas/" + empresa + "/sucursales")
						.header("Authorization", "Bearer " + dueno).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}
}
