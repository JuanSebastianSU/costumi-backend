package com.costumi.backend.ventas;

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

/** El listado de ventas se acota a la sucursal activa (cabecera X-Sucursal-Id); sin ella, todas (Fase B). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VentaFiltroSucursalIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void las_ventas_se_filtran_por_la_sucursal_activa() throws Exception {
		UUID empresa = crearEmpresaAprobada();
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		// Habilita multi-sucursal (para tener dos) y apaga el conteo de stock (así la venta no necesita grupos).
		mvc.perform(put("/api/v1/configuracion").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"conteoStock\":false,\"multasActivo\":true,\"multiSucursal\":true,\"pagoEnLinea\":false}"))
				.andExpect(status().isOk());
		UUID sucA = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"A\"}");
		UUID sucB = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"B\"}");
		UUID cat = postId("/api/v1/categorias", dueno, "{\"nombre\":\"C " + UUID.randomUUID() + "\"}");
		UUID prenda = postId("/api/v1/prendas", dueno, "{\"categoriaId\":\"" + cat
				+ "\",\"nombre\":\"P\",\"tipoArticulo\":\"VENTA\",\"precioVenta\":90.00}");
		// Una venta en cada sucursal (sin conteo de stock, no hacen falta grupos).
		vender(dueno, sucA, prenda);
		vender(dueno, sucB, prenda);

		// Sin cabecera: las dos ventas de la empresa.
		mvc.perform(get("/api/v1/ventas").header("Authorization", "Bearer " + dueno))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(2));

		// Con X-Sucursal-Id = A: solo la de A (lista y totales).
		mvc.perform(get("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.header("X-Sucursal-Id", sucA.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(1));
		mvc.perform(get("/api/v1/ventas/totales").header("Authorization", "Bearer " + dueno)
						.header("X-Sucursal-Id", sucA.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cantidad").value(1));
	}

	private void vender(String dueno, UUID sucursal, UUID prenda) throws Exception {
		mvc.perform(post("/api/v1/ventas").header("Authorization", "Bearer " + dueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"sucursalId\":\"" + sucursal + "\",\"lineas\":[{\"prendaId\":\"" + prenda
								+ "\",\"cantidad\":1,\"precioUnitario\":90.00}]}"))
				.andExpect(status().isCreated());
	}

	private UUID crearEmpresaAprobada() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"VS " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		return empresa;
	}

	private UUID postId(String path, String token, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}
}
