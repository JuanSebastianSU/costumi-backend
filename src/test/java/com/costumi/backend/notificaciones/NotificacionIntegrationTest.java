package com.costumi.backend.notificaciones;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.identidad.AuthTestHelper;
import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import com.costumi.backend.notificaciones.aplicacion.RecordarVencidas;
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

/** Notificaciones (RF-11): se envían (canal log) y quedan ENVIADA, acotado al tenant. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class NotificacionIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	RecordarVencidas recordarVencidas;

	@Test
	void enviar_una_notificacion_la_deja_enviada() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Notif " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);

		mvc.perform(post("/api/v1/notificaciones").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"canal\":\"WHATSAPP\",\"mensaje\":\"Tu renta vence mañana\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.estado").value("ENVIADA"))
				.andExpect(jsonPath("$.canal").value("WHATSAPP"));

		mvc.perform(get("/api/v1/notificaciones").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido.length()").value(1));
	}

	@Test
	void avisar_stock_bajo_deja_un_resumen_in_app_al_dueno() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Stock " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"Centro\"}");
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Traje\",\"tipoArticulo\":\"RENTA\",\"precioRenta\":20.00}");
		// Grupo con 0 unidades disponibles: queda por debajo del umbral por defecto (1).
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno,
				"{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":0}");

		// RF-11.2: dispara el aviso proactivo -> un resumen in-app para el dueño.
		mvc.perform(post("/api/v1/notificaciones/avisar-stock-bajo").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enviadas").value(1));

		mvc.perform(get("/api/v1/notificaciones").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.canal == 'IN_APP')]").exists());
	}

	private UUID postId(String path, String tk, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + tk)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}

	@Test
	void recordar_vencidas_avisa_a_cada_cliente_con_renta_vencida() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Venc " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"Centro\"}");
		UUID cliente = postId("/api/v1/clientes", dueno, "{\"nombre\":\"Cliente\"}");
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Traje\",\"tipoArticulo\":\"RENTA\",\"precioRenta\":20.00}");
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno,
				"{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":2}");
		UUID renta = postId("/api/v1/rentas", dueno, "{\"sucursalId\":\"" + sucursal + "\",\"clienteId\":\"" + cliente
				+ "\",\"prendaId\":\"" + prenda + "\",\"fechaRetiro\":\"2020-01-01\",\"fechaDevolucion\":\"2020-01-05\","
				+ "\"precioPorDia\":20.00,\"deposito\":100.00}");
		mvc.perform(post("/api/v1/rentas/{id}/entregar", renta).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk());

		// RF-11.1: dispara los recordatorios -> avisa al cliente (WhatsApp) Y al dueño (resumen in-app) = 2.
		mvc.perform(post("/api/v1/notificaciones/recordar-vencidas").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enviadas").value(2));

		// Quedó registrada la notificación al cliente (WhatsApp, plantilla configurable)...
		mvc.perform(get("/api/v1/notificaciones").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.canal == 'WHATSAPP')]").exists())
				// ...y el resumen in-app para el dueño (sin cliente asociado).
				.andExpect(jsonPath("$.contenido[?(@.canal == 'IN_APP')]").exists());
	}

	@Test
	void empresas_con_vencidas_lista_la_empresa_con_renta_vencida() throws Exception {
		// RF-3.5: es lo que usa el job programado para saber a qué empresas recordar (sin token).
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Sched " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"Centro\"}");
		UUID cliente = postId("/api/v1/clientes", dueno, "{\"nombre\":\"Cliente\"}");
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Traje\",\"tipoArticulo\":\"RENTA\",\"precioRenta\":20.00}");
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno,
				"{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":2}");
		UUID renta = postId("/api/v1/rentas", dueno, "{\"sucursalId\":\"" + sucursal + "\",\"clienteId\":\"" + cliente
				+ "\",\"prendaId\":\"" + prenda + "\",\"fechaRetiro\":\"2020-01-01\",\"fechaDevolucion\":\"2020-01-05\","
				+ "\"precioPorDia\":20.00,\"deposito\":100.00}");
		mvc.perform(post("/api/v1/rentas/{id}/entregar", renta).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk());

		// La empresa con renta vencida aparece en la lista que recorre el scheduler.
		org.assertj.core.api.Assertions.assertThat(recordarVencidas.empresasConVencidas()).contains(empresa);
	}

	@Test
	void recordar_proximas_avisa_a_los_clientes_cuya_renta_vence_pronto() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Prox " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno,
				"{\"nombre\":\"Centro\",\"direccion\":\"Av Central 100\",\"ubicacionMaps\":\"https://maps.app.goo.gl/x\"}");
		UUID cliente = postId("/api/v1/clientes", dueno, "{\"nombre\":\"Ana\"}");
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + categoria
				+ "\",\"nombre\":\"Traje\",\"tipoArticulo\":\"RENTA\",\"precioRenta\":20.00}");
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno,
				"{\"sucursalId\":\"" + sucursal + "\",\"combinacion\":[],\"cantidadInicial\":2}");
		// La renta vence MAÑANA (dentro de la ventana por defecto de 1 día).
		String hoy = java.time.LocalDate.now().toString();
		String manana = java.time.LocalDate.now().plusDays(1).toString();
		UUID renta = postId("/api/v1/rentas", dueno, "{\"sucursalId\":\"" + sucursal + "\",\"clienteId\":\"" + cliente
				+ "\",\"prendaId\":\"" + prenda + "\",\"fechaRetiro\":\"" + hoy + "\",\"fechaDevolucion\":\"" + manana
				+ "\",\"precioPorDia\":20.00,\"deposito\":100.00}");
		mvc.perform(post("/api/v1/rentas/{id}/entregar", renta).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk());

		// RF-11.1: dispara el recordatorio anticipado -> 1 enviado.
		mvc.perform(post("/api/v1/notificaciones/recordar-proximas").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enviadas").value(1));

		// El mensaje usa la plantilla configurable con cliente, días restantes y dirección/maps.
		mvc.perform(get("/api/v1/notificaciones").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.contenido[?(@.canal == 'WHATSAPP')].mensaje",
						org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("Hola Ana"))))
				.andExpect(jsonPath("$.contenido[?(@.canal == 'WHATSAPP')].mensaje",
						org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("Av Central 100"))));
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/notificaciones")).andExpect(status().isUnauthorized());
	}

	@Test
	void el_estado_de_canales_dice_por_que_un_aviso_cae_al_log() throws Exception {
		// Sin credencial de FCM configurada (el caso por defecto en tests), el canal no esta listo y el
		// diagnostico lo dice con todas las letras: es lo que evita creer que una push "ENVIADA" llego.
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Canales " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		mvc.perform(get("/api/v1/notificaciones/estado-canales").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fcmConfigurado").value(false))
				.andExpect(jsonPath("$.fcmDetalle").value(org.hamcrest.Matchers.containsString("COSTUMI_FCM_CREDENTIALS")));
	}
}
