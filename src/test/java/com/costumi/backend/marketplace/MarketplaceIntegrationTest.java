package com.costumi.backend.marketplace;

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

/** Marketplace (RF-18.1/RF-15.6): solo las empresas ACTIVAS son visibles públicamente. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MarketplaceIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private UUID crearEmpresa(String nombre) throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	/** Tienda por el flujo real del marketplace: un CLIENTE la solicita, así al aprobarla nace su Casa Matriz. */
	private UUID crearTiendaConSolicitante(String nombre) throws Exception {
		String cliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);
		String res = mvc.perform(post("/api/v1/empresas").header("Authorization", "Bearer " + cliente)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	@Test
	void solo_las_empresas_activas_aparecen_en_la_vitrina() throws Exception {
		String activa = "Activa-" + UUID.randomUUID();
		String pendiente = "Pendiente-" + UUID.randomUUID();
		UUID empresaActiva = crearTiendaConSolicitante(activa); // al aprobar tendrá Casa Matriz -> operable
		crearEmpresa(pendiente); // queda PENDIENTE

		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresaActiva)
						.header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());

		// Endpoint público: sin token.
		mvc.perform(get("/api/v1/marketplace/empresas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.nombre == '" + activa + "')]").exists())
				.andExpect(jsonPath("$[?(@.nombre == '" + pendiente + "')]").doesNotExist());
	}

	@Test
	void una_tienda_sin_sucursal_activa_no_aparece_en_la_vitrina() throws Exception {
		// Una tienda que no puede recibir pedidos (sin punto de retiro) no debe ofrecerse al cliente.
		String nombre = "SinSuc-" + UUID.randomUUID();
		UUID empresa = crearTiendaConSolicitante(nombre);
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());

		// Con su Casa Matriz recién creada, la tienda aparece.
		mvc.perform(get("/api/v1/marketplace/empresas"))
				.andExpect(jsonPath("$[?(@.nombre == '" + nombre + "')]").exists());

		// Se archiva su única sucursal (vacía, sin inventario) -> ya no puede operar.
		String sucJson = mvc.perform(get("/api/v1/marketplace/empresas/{id}/sucursales", empresa))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		UUID sucursalId = UUID.fromString(json.readTree(sucJson).get(0).get("id").asText());
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		mvc.perform(post("/api/v1/empresas/{e}/sucursales/{s}/archivar", empresa, sucursalId)
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk());

		// Ya no aparece en la vitrina.
		mvc.perform(get("/api/v1/marketplace/empresas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.nombre == '" + nombre + "')]").doesNotExist());
	}

	@Test
	void la_vitrina_se_puede_buscar_por_texto() throws Exception {
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		String marca = "Disfraces" + UUID.randomUUID().toString().substring(0, 8);
		UUID coincide = crearTiendaConSolicitante(marca + " Centro");
		UUID otra = crearTiendaConSolicitante("Trajes " + UUID.randomUUID());
		for (UUID e : new UUID[] { coincide, otra }) {
			mvc.perform(post("/api/v1/empresas/{id}/aprobar", e).header("Authorization", "Bearer " + superAdmin))
					.andExpect(status().isOk());
		}

		// Búsqueda por texto (RF-18.1): solo la que coincide en el nombre.
		mvc.perform(get("/api/v1/marketplace/empresas").param("buscar", marca))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.nombre == '" + marca + " Centro')]").exists())
				.andExpect(jsonPath("$.length()").value(1));
	}

	/**
	 * RF-18.5: el cliente del marketplace necesita elegir la sucursal (punto de retiro) para armar su
	 * carrito. Debe poder listarlas sin token (vitrina pública). Al aprobar una empresa se crea su
	 * Casa Matriz, que aparece aquí.
	 */
	@Test
	void las_sucursales_de_una_tienda_activa_son_publicas() throws Exception {
		// Flujo real de onboarding: un CLIENTE pide abrir su tienda (con token) → al aprobar se
		// crea la Casa Matriz (solo se provisiona cuando la empresa viene de una solicitud).
		String cliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);
		String nombre = "Tienda-" + UUID.randomUUID();
		String res = mvc.perform(post("/api/v1/empresas").header("Authorization", "Bearer " + cliente)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());

		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa)
						.header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());

		// Endpoint público (sin token): aparece la Casa Matriz creada al aprobar.
		mvc.perform(get("/api/v1/marketplace/empresas/{id}/sucursales", empresa))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").exists())
				.andExpect(jsonPath("$[0].nombre").value("Casa Matriz"));
	}

	/** Una tienda que no está ACTIVA (p. ej. pendiente de aprobación) no expone sus sucursales. */
	@Test
	void una_tienda_no_activa_no_expone_sucursales() throws Exception {
		UUID empresa = crearEmpresa("Pendiente-" + UUID.randomUUID()); // queda PENDIENTE

		mvc.perform(get("/api/v1/marketplace/empresas/{id}/sucursales", empresa))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}
}
