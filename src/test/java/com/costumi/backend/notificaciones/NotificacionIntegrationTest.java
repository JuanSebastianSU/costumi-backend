package com.costumi.backend.notificaciones;

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
				.andExpect(jsonPath("$.length()").value(1));
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
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", dueno, "{\"combinacion\":[],\"cantidadInicial\":2}");
		UUID renta = postId("/api/v1/rentas", dueno, "{\"sucursalId\":\"" + sucursal + "\",\"clienteId\":\"" + cliente
				+ "\",\"prendaId\":\"" + prenda + "\",\"fechaRetiro\":\"2020-01-01\",\"fechaDevolucion\":\"2020-01-05\","
				+ "\"precioPorDia\":20.00,\"deposito\":100.00}");
		mvc.perform(post("/api/v1/rentas/{id}/entregar", renta).header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk());

		// RF-11.1: dispara los recordatorios -> 1 enviado.
		mvc.perform(post("/api/v1/notificaciones/recordar-vencidas").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enviadas").value(1));

		// Quedó registrada la notificación de recordatorio (canal EMAIL).
		mvc.perform(get("/api/v1/notificaciones").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.canal == 'EMAIL')]").exists());
	}

	@Test
	void sin_token_devuelve_401() throws Exception {
		mvc.perform(get("/api/v1/notificaciones")).andExpect(status().isUnauthorized());
	}
}
