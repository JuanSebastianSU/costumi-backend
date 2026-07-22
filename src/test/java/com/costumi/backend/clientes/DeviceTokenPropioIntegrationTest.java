package com.costumi.backend.clientes;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El propio usuario registra el token de su dispositivo (RF-18.11). Antes solo existia el endpoint por id,
 * que exige el claim {@code empresa_id} y un rol de personal: un CLIENTE del marketplace no tiene ninguno
 * de los dos, asi que no habia forma de que la app registrara el telefono y las push nunca podian llegar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DeviceTokenPropioIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void un_cliente_registra_el_token_de_su_dispositivo_en_todas_sus_fichas() throws Exception {
		UUID empresaA = empresaAprobada();
		UUID empresaB = empresaAprobada();
		String cliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);

		// Al comprar en cada tienda se le crea una ficha; aqui basta con tocar su historial de cada una.
		crearFicha(cliente, empresaA);
		crearFicha(cliente, empresaB);

		mvc.perform(put("/api/v1/clientes/me/device-token").header("Authorization", "Bearer " + cliente)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"deviceToken\":\"token-de-prueba-123\"}"))
				.andExpect(status().isNoContent());

		// Quedo en las fichas de las DOS tiendas: el telefono es el mismo para todas.
		for (UUID empresa : new UUID[] { empresaA, empresaB }) {
			String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
			mvc.perform(get("/api/v1/clientes").header("Authorization", "Bearer " + dueno))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.contenido.length()").value(1));
		}
	}

	@Test
	void sin_token_no_se_puede_registrar_un_dispositivo() throws Exception {
		mvc.perform(put("/api/v1/clientes/me/device-token").contentType(MediaType.APPLICATION_JSON)
						.content("{\"deviceToken\":\"lo-que-sea\"}"))
				.andExpect(status().isUnauthorized());
	}

	/** Toca el carrito de la tienda: eso resuelve/crea la ficha del usuario en esa empresa (RF-14.4). */
	private void crearFicha(String tokenCliente, UUID empresa) throws Exception {
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		String res = mvc.perform(post("/api/v1/empresas/{id}/sucursales", empresa)
						.header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Centro\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID sucursal = UUID.fromString(json.readTree(res).get("id").asText());
		// Un GET del carrito basta: resuelve la ficha del usuario en esa empresa aunque no haya carrito.
		mvc.perform(get("/api/v1/carritos").header("Authorization", "Bearer " + tokenCliente)
				.param("empresaId", empresa.toString())
				.param("sucursalId", sucursal.toString())
				.param("tipo", "VENTA"));
	}

	private UUID empresaAprobada() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Push " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		return empresa;
	}
}
