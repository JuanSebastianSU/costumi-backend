package com.costumi.backend.configuracion;

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

/** Configuración de empresa (RF-12): defaults, actualización y permisos, acotado al tenant. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ConfiguracionIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private UUID empresa;

	private String tokenRol(Rol rol) throws Exception {
		return AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, rol);
	}

	private void montar() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Cfg " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		this.empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
	}

	@Test
	void obtiene_defaults_y_actualiza() throws Exception {
		montar();
		String dueno = tokenRol(Rol.DUENO);

		mvc.perform(get("/api/v1/configuracion").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.conteoStock").value(true))
				.andExpect(jsonPath("$.multiSucursal").value(false))
				.andExpect(jsonPath("$.tasaImpuesto").value(0)); // impuesto por defecto 0 (RF-6.5)

		mvc.perform(put("/api/v1/configuracion").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"conteoStock\":false,\"multasActivo\":true,\"multiSucursal\":true,\"pagoEnLinea\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.multiSucursal").value(true));

		mvc.perform(get("/api/v1/configuracion").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.multiSucursal").value(true))
				.andExpect(jsonPath("$.conteoStock").value(false));
	}

	@Test
	void configura_la_tasa_de_impuesto_de_la_empresa() throws Exception {
		montar();
		String dueno = tokenRol(Rol.DUENO);

		mvc.perform(put("/api/v1/configuracion").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"conteoStock\":true,\"multasActivo\":true,\"multiSucursal\":false,"
								+ "\"pagoEnLinea\":false,\"tasaImpuesto\":0.19}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tasaImpuesto").value(0.19));

		mvc.perform(get("/api/v1/configuracion").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tasaImpuesto").value(0.19));
	}

	@Test
	void un_rol_sin_permiso_no_puede_actualizar_403() throws Exception {
		montar();
		String mostrador = tokenRol(Rol.MOSTRADOR);

		mvc.perform(put("/api/v1/configuracion").header("Authorization", "Bearer " + mostrador)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"conteoStock\":false,\"multasActivo\":false,\"multiSucursal\":false,\"pagoEnLinea\":false}"))
				.andExpect(status().isForbidden());
	}
}
