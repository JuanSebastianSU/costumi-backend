package com.costumi.backend.identidad;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
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

/** Alta de Sucursal (RF-15.1): rol DUENO/ENCARGADO + dueño del tenant (RF-15.4). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegistrarSucursalIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private UUID registrarEmpresa(String nombre) throws Exception {
		String body = mvc.perform(post("/api/v1/empresas")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		JsonNode node = json.readTree(body);
		return UUID.fromString(node.get("id").asText());
	}

	private void aprobar(UUID empresaId) throws Exception {
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresaId)
						.header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
	}

	private String tokenDueno(UUID empresaId) throws Exception {
		return AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresaId, Rol.DUENO);
	}

	@Test
	void el_dueno_da_de_alta_una_sucursal_en_su_empresa_activa() throws Exception {
		UUID empresaId = registrarEmpresa("Empresa Activa");
		aprobar(empresaId);
		String dueno = tokenDueno(empresaId);

		mvc.perform(post("/api/v1/empresas/{empresaId}/sucursales", empresaId)
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Centro\",\"direccion\":\"Calle 1\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.empresaId").value(empresaId.toString()))
				.andExpect(jsonPath("$.nombre").value("Centro"));
	}

	@Test
	void en_empresa_pendiente_devuelve_409() throws Exception {
		UUID empresaId = registrarEmpresa("Empresa Pendiente");
		String dueno = tokenDueno(empresaId);

		mvc.perform(post("/api/v1/empresas/{empresaId}/sucursales", empresaId)
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Centro\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void el_dueno_de_otra_empresa_no_puede_abrir_sucursal_ajena_403() throws Exception {
		UUID empresaAjena = registrarEmpresa("Empresa Ajena");
		UUID empresaPropia = registrarEmpresa("Empresa Propia");
		String duenoDeOtra = tokenDueno(empresaPropia);

		mvc.perform(post("/api/v1/empresas/{empresaId}/sucursales", empresaAjena)
						.header("Authorization", "Bearer " + duenoDeOtra)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Centro\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void sin_multi_sucursal_la_segunda_devuelve_409_y_con_el_switch_on_se_permite() throws Exception {
		UUID empresaId = registrarEmpresa("Multi " + UUID.randomUUID());
		aprobar(empresaId);
		String dueno = tokenDueno(empresaId);

		// Primera sucursal: siempre permitida.
		mvc.perform(post("/api/v1/empresas/{empresaId}/sucursales", empresaId).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Centro\"}"))
				.andExpect(status().isCreated());

		// Segunda con multi-sucursal APAGADO (por defecto): 409 (RF-12.4).
		mvc.perform(post("/api/v1/empresas/{empresaId}/sucursales", empresaId).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Norte\"}"))
				.andExpect(status().isConflict());

		// Enciende multi-sucursal.
		mvc.perform(put("/api/v1/configuracion").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"conteoStock\":true,\"multasActivo\":true,\"multiSucursal\":true,\"pagoEnLinea\":false}"))
				.andExpect(status().isOk());

		// Ahora sí se permite la segunda sucursal.
		mvc.perform(post("/api/v1/empresas/{empresaId}/sucursales", empresaId).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Norte\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		UUID empresaId = registrarEmpresa("Sin token");

		mvc.perform(post("/api/v1/empresas/{empresaId}/sucursales", empresaId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Centro\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void lista_las_sucursales_de_la_empresa() throws Exception {
		UUID empresaId = registrarEmpresa("Con Sucursal " + UUID.randomUUID());
		aprobar(empresaId);
		String dueno = tokenDueno(empresaId);
		mvc.perform(post("/api/v1/empresas/{empresaId}/sucursales", empresaId).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Centro\"}"))
				.andExpect(status().isCreated());

		mvc.perform(get("/api/v1/empresas/{empresaId}/sucursales", empresaId)
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].empresaId").value(empresaId.toString()))
				.andExpect(jsonPath("$[0].nombre").value("Centro"));
	}

	@Test
	void mostrador_del_tenant_puede_listar_sucursales() throws Exception {
		UUID empresaId = registrarEmpresa("Mostrador Lista " + UUID.randomUUID());
		aprobar(empresaId);
		String dueno = tokenDueno(empresaId);
		mvc.perform(post("/api/v1/empresas/{empresaId}/sucursales", empresaId).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Centro\"}"))
				.andExpect(status().isCreated());

		String mostrador = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresaId, Rol.MOSTRADOR);
		mvc.perform(get("/api/v1/empresas/{empresaId}/sucursales", empresaId)
						.header("Authorization", "Bearer " + mostrador))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].nombre").value("Centro"));
	}

	@Test
	void no_puede_listar_sucursales_de_otra_empresa_403() throws Exception {
		UUID empresaAjena = registrarEmpresa("Ajena Lista " + UUID.randomUUID());
		UUID empresaPropia = registrarEmpresa("Propia Lista " + UUID.randomUUID());
		String duenoDeOtra = tokenDueno(empresaPropia);

		mvc.perform(get("/api/v1/empresas/{empresaId}/sucursales", empresaAjena)
						.header("Authorization", "Bearer " + duenoDeOtra))
				.andExpect(status().isForbidden());
	}

	@Test
	void editar_una_sucursal_actualiza_nombre_y_direccion() throws Exception {
		UUID empresaId = registrarEmpresa("Editar Suc " + UUID.randomUUID());
		aprobar(empresaId);
		String dueno = tokenDueno(empresaId);
		UUID sucursal = crearSucursal(dueno, empresaId, "Centro");

		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.patch("/api/v1/empresas/{empresaId}/sucursales/{id}", empresaId, sucursal)
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Centro Renovado\",\"direccion\":\"Av. Nueva 99\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Centro Renovado"))
				.andExpect(jsonPath("$.direccion").value("Av. Nueva 99"));
	}

	@Test
	void archivar_una_sucursal_sin_dependencias_y_reactivar() throws Exception {
		UUID empresaId = registrarEmpresa("Archivar Suc " + UUID.randomUUID());
		aprobar(empresaId);
		String dueno = tokenDueno(empresaId);
		UUID sucursal = crearSucursal(dueno, empresaId, "Centro");

		mvc.perform(post("/api/v1/empresas/{empresaId}/sucursales/{id}/archivar", empresaId, sucursal)
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.archivada").value(true));

		mvc.perform(post("/api/v1/empresas/{empresaId}/sucursales/{id}/activar", empresaId, sucursal)
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.archivada").value(false));
	}

	@Test
	void no_se_puede_archivar_una_sucursal_con_stock_409() throws Exception {
		UUID empresaId = registrarEmpresa("Suc Con Stock " + UUID.randomUUID());
		aprobar(empresaId);
		String dueno = tokenDueno(empresaId);
		UUID sucursal = crearSucursal(dueno, empresaId, "Centro");

		// Sembrar stock en esa sucursal (categoría -> prenda -> grupo de stock). Nombre único: el alta de
		// empresa ya siembra categorías por defecto (evita chocar con el índice único de nombre).
		UUID categoria = crearCategoria(dueno, "Cat " + UUID.randomUUID());
		String p = mvc.perform(post("/api/v1/prendas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoriaId\":\"" + categoria + "\",\"nombre\":\"Traje\","
								+ "\"tipoArticulo\":\"RENTA\",\"precioRenta\":40.00}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID prenda = UUID.fromString(json.readTree(p).get("id").asText());
		mvc.perform(post("/api/v1/prendas/{id}/grupos-stock", prenda).header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":3}"))
				.andExpect(status().isCreated());

		// Archivar la sucursal con stock: 409 con el conteo.
		mvc.perform(post("/api/v1/empresas/{empresaId}/sucursales/{id}/archivar", empresaId, sucursal)
						.header("Authorization", "Bearer " + dueno))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.unidadesStock").value(3))
				.andExpect(jsonPath("$.rentasVigentes").value(0));
	}

	@Test
	void un_rol_sin_permiso_no_puede_archivar_una_sucursal_403() throws Exception {
		UUID empresaId = registrarEmpresa("Suc Rol " + UUID.randomUUID());
		aprobar(empresaId);
		String dueno = tokenDueno(empresaId);
		UUID sucursal = crearSucursal(dueno, empresaId, "Centro");

		String mostrador = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresaId, Rol.MOSTRADOR);
		mvc.perform(post("/api/v1/empresas/{empresaId}/sucursales/{id}/archivar", empresaId, sucursal)
						.header("Authorization", "Bearer " + mostrador))
				.andExpect(status().isForbidden());
	}

	private UUID crearSucursal(String token, UUID empresaId, String nombre) throws Exception {
		String body = mvc.perform(post("/api/v1/empresas/{empresaId}/sucursales", empresaId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}

	private UUID crearCategoria(String token, String nombre) throws Exception {
		String body = mvc.perform(post("/api/v1/categorias").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"" + nombre + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(body).get("id").asText());
	}
}
