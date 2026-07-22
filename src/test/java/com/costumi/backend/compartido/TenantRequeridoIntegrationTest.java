package com.costumi.backend.compartido;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los endpoints de gestión exigen un usuario con empresa (§5.4). Un CLIENTE del marketplace está
 * autenticado pero su token <b>no trae</b> {@code empresa_id}, y varias de estas rutas caen en el
 * {@code anyRequest().authenticated()} de la configuración de seguridad, así que las alcanza.
 *
 * <p>Antes, cada controller hacía {@code UUID.fromString(jwt.getClaimAsString("empresa_id"))}: con el
 * claim ausente eso reventaba en un NullPointerException y la respuesta era un <b>500</b>, que disfraza
 * un problema de permisos como una caída del servidor (y ensucia los logs). Ahora usan
 * {@link ContextoDeTenant#empresaIdRequerida()}, que responde <b>403</b>.
 *
 * <p>No había fuga de datos en ningún caso: la petición moría antes de tocar la base. Esto arregla el
 * código de estado, no un agujero de aislamiento.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TenantRequeridoIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void un_cliente_recibe_403_en_los_endpoints_que_exigen_empresa() throws Exception {
		String cliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);
		UUID cualquiera = UUID.randomUUID();

		String[] rutas = {
			"/api/v1/rentas/" + cualquiera,
			"/api/v1/ventas/" + cualquiera,
			"/api/v1/clientes/" + cualquiera + "/historial",
			"/api/v1/clientes/" + cualquiera + "/estado-cuenta",
			"/api/v1/empleados/" + cualquiera + "/actividad",
			"/api/v1/categorias/" + cualquiera + "/prendas/conteo",
			"/api/v1/notificaciones/plantillas",
		};
		for (String ruta : rutas) {
			mvc.perform(get(ruta).header("Authorization", "Bearer " + cliente))
					.andExpect(status().isForbidden());
		}
	}

	/**
	 * Las listas de gestión ya devolvían vacío para un usuario sin empresa (usan {@code empresaId()},
	 * el opcional) y deben seguir haciéndolo: este cambio no las convierte en 403.
	 */
	@Test
	void las_listas_de_gestion_siguen_devolviendo_vacio_sin_empresa() throws Exception {
		String cliente = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.CLIENTE);

		mvc.perform(get("/api/v1/prendas").header("Authorization", "Bearer " + cliente))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v1/categorias").header("Authorization", "Bearer " + cliente))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v1/notificaciones").header("Authorization", "Bearer " + cliente))
				.andExpect(status().isOk());
	}
}
