package com.costumi.backend.disfraces;

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

/**
 * Coherencia entre el tipo del disfraz y el de sus piezas (RF-2.1 + RF-2.3).
 *
 * <p>Sin esto se podían armar disfraces que no se podían cobrar: una prenda de solo renta no tiene precio
 * de venta, así que un disfraz "de venta" hecho con ellas salía valuado en <b>0</b>, en silencio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TipoDeDisfrazCoherenteIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	@Autowired
	UsuarioRepository usuarios;

	@Autowired
	PasswordEncoder passwordEncoder;

	private record Tienda(UUID empresa, String dueno, UUID sucursal, UUID categoria) {
	}

	// --- El tipo se DERIVA de las piezas cuando el dueño no lo elige ---

	@Test
	void sin_elegir_tipo_un_disfraz_de_prendas_de_renta_queda_de_renta() throws Exception {
		Tienda t = montar();
		UUID soloRenta = prenda(t, "RENTA", "\"precioRenta\":40.00");

		crearDisfraz(t, soloRenta, null)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipo").value("RENTA"));
	}

	@Test
	void sin_elegir_tipo_un_disfraz_de_prendas_de_venta_queda_de_venta() throws Exception {
		Tienda t = montar();
		UUID soloVenta = prenda(t, "VENTA", "\"precioVenta\":90.00");

		crearDisfraz(t, soloVenta, null)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipo").value("VENTA"));
	}

	@Test
	void sin_elegir_tipo_un_disfraz_de_prendas_de_ambos_queda_de_ambos() throws Exception {
		Tienda t = montar();
		UUID ambos = prenda(t, "AMBOS", "\"precioRenta\":40.00,\"precioVenta\":90.00");

		crearDisfraz(t, ambos, null)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipo").value("AMBOS"));
	}

	/**
	 * Una pieza de solo renta y otra de solo venta: el disfraz no serviría para nada, porque no se podría
	 * rentar (una pieza no se renta) ni vender (la otra no se vende).
	 */
	@Test
	void mezclar_piezas_de_solo_renta_y_solo_venta_se_rechaza() throws Exception {
		Tienda t = montar();
		UUID soloRenta = prenda(t, "RENTA", "\"precioRenta\":40.00");
		UUID soloVenta = prenda(t, "VENTA", "\"precioVenta\":90.00");

		mvc.perform(post("/api/v1/disfraces").header("Authorization", "Bearer " + t.dueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Mezcla\",\"slots\":["
								+ "{\"orden\":1,\"nombre\":\"A\",\"ejePrenda\":\"FIJA\",\"prendaFijaId\":\"" + soloRenta
								+ "\",\"opcional\":false},"
								+ "{\"orden\":2,\"nombre\":\"B\",\"ejePrenda\":\"FIJA\",\"prendaFijaId\":\"" + soloVenta
								+ "\",\"opcional\":false}]}"))
				.andExpect(status().isUnprocessableEntity());
	}

	// --- Si el dueño SÍ elige el tipo, las piezas tienen que servir para eso ---

	@Test
	void un_disfraz_de_venta_no_admite_una_prenda_de_solo_renta() throws Exception {
		Tienda t = montar();
		UUID soloRenta = prenda(t, "RENTA", "\"precioRenta\":40.00");

		crearDisfraz(t, soloRenta, "VENTA")
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void un_disfraz_de_renta_no_admite_una_prenda_de_solo_venta() throws Exception {
		Tienda t = montar();
		UUID soloVenta = prenda(t, "VENTA", "\"precioVenta\":90.00");

		crearDisfraz(t, soloVenta, "RENTA")
				.andExpect(status().isUnprocessableEntity());
	}

	/** AMBOS es el caso mas exigente: la pieza tiene que servir para las dos cosas. */
	@Test
	void un_disfraz_de_ambos_exige_piezas_de_ambos() throws Exception {
		Tienda t = montar();
		UUID soloRenta = prenda(t, "RENTA", "\"precioRenta\":40.00");

		crearDisfraz(t, soloRenta, "AMBOS")
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void un_disfraz_de_renta_si_admite_una_prenda_de_ambos() throws Exception {
		Tienda t = montar();
		UUID ambos = prenda(t, "AMBOS", "\"precioRenta\":40.00,\"precioVenta\":90.00");

		crearDisfraz(t, ambos, "RENTA")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipo").value("RENTA"));
	}

	// --- La ruleta no ofrece opciones que el disfraz no puede usar ---

	/**
	 * El pool NO se valida al crear (es dinámico: mañana alguien agrega otra prenda a esa categoría), así
	 * que la coherencia se garantiza filtrando la ruleta.
	 */
	@Test
	void la_ruleta_no_ofrece_prendas_que_el_disfraz_no_puede_usar() throws Exception {
		Tienda t = montar();
		UUID ambos = prenda(t, "AMBOS", "\"precioRenta\":40.00,\"precioVenta\":90.00");
		UUID soloVenta = prenda(t, "VENTA", "\"precioVenta\":90.00");
		stock(t, ambos);
		stock(t, soloVenta);

		// Disfraz de RENTA con un slot de pool sobre la categoria (que tiene las dos prendas).
		UUID disfraz = postId("/api/v1/disfraces", t.dueno(), "{\"nombre\":\"Pirata\",\"tipo\":\"RENTA\","
				+ "\"slots\":[{\"orden\":1,\"nombre\":\"Traje\",\"ejePrenda\":\"PERSONALIZABLE\","
				+ "\"pool\":{\"categoriaId\":\"" + t.categoria() + "\",\"etiquetasPermitidas\":[]},"
				+ "\"opcional\":false}]}");

		// Solo aparece la que se puede rentar; la de solo venta queda fuera.
		mvc.perform(get("/api/v1/marketplace/empresas/{e}/disfraces/{d}/slots/{o}/opciones",
						t.empresa(), disfraz, 1))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.opciones.length()").value(1))
				.andExpect(jsonPath("$.opciones[0].prendaId").value(ambos.toString()));
	}

	// --- helpers ---

	private org.springframework.test.web.servlet.ResultActions crearDisfraz(Tienda t, UUID prenda, String tipo)
			throws Exception {
		String campoTipo = tipo == null ? "" : "\"tipo\":\"" + tipo + "\",";
		return mvc.perform(post("/api/v1/disfraces").header("Authorization", "Bearer " + t.dueno())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Disfraz " + UUID.randomUUID() + "\"," + campoTipo
						+ "\"slots\":[{\"orden\":1,\"nombre\":\"Traje\",\"ejePrenda\":\"FIJA\","
						+ "\"prendaFijaId\":\"" + prenda + "\",\"opcional\":false}]}"));
	}

	private UUID prenda(Tienda t, String tipoArticulo, String precios) throws Exception {
		return postId("/api/v1/prendas", t.dueno(), "{\"categoriaId\":\"" + t.categoria()
				+ "\",\"nombre\":\"P " + UUID.randomUUID() + "\",\"tipoArticulo\":\"" + tipoArticulo + "\","
				+ precios + "}");
	}

	private void stock(Tienda t, UUID prenda) throws Exception {
		postId("/api/v1/prendas/" + prenda + "/grupos-stock", t.dueno(),
				"{\"sucursalId\":\"" + t.sucursal() + "\",\"combinacion\":[],\"cantidadInicial\":3}");
	}

	private Tienda montar() throws Exception {
		String res = mvc.perform(post("/api/v1/empresas").contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Tipo " + UUID.randomUUID() + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		UUID empresa = UUID.fromString(json.readTree(res).get("id").asText());
		String superAdmin = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, null, Rol.SUPERADMIN);
		mvc.perform(post("/api/v1/empresas/{id}/aprobar", empresa).header("Authorization", "Bearer " + superAdmin))
				.andExpect(status().isOk());
		String dueno = AuthTestHelper.token(mvc, json, usuarios, passwordEncoder, empresa, Rol.DUENO);
		UUID sucursal = postId("/api/v1/empresas/" + empresa + "/sucursales", dueno, "{\"nombre\":\"Centro\"}");
		UUID categoria = postId("/api/v1/categorias", dueno, "{\"nombre\":\"Cat " + UUID.randomUUID() + "\"}");
		return new Tienda(empresa, dueno, sucursal, categoria);
	}

	private UUID postId(String path, String token, String body) throws Exception {
		String res = mvc.perform(post(path).header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return UUID.fromString(json.readTree(res).get("id").asText());
	}
}
