package com.costumi.backend.identidad;

import com.costumi.backend.identidad.dominio.Rol;
import com.costumi.backend.identidad.dominio.Usuario;
import com.costumi.backend.identidad.dominio.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Utilidad de test: siembra un usuario con el rol dado y devuelve un token de acceso. */
public final class AuthTestHelper {

	private AuthTestHelper() {
	}

	public static String token(MockMvc mvc, ObjectMapper json, UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
			UUID empresaId, Rol rol) throws Exception {
		return sesion(mvc, json, usuarios, passwordEncoder, empresaId, rol).token();
	}

	/** El id del usuario sembrado y su token de acceso (para pruebas que necesitan ambos, p. ej. permisos). */
	public record Sesion(UUID usuarioId, String token) {
	}

	public static Sesion sesion(MockMvc mvc, ObjectMapper json, UsuarioRepository usuarios,
			PasswordEncoder passwordEncoder, UUID empresaId, Rol rol) throws Exception {
		String email = "u-" + UUID.randomUUID() + "@costumi.test";
		Usuario usuario = Usuario.crear(empresaId, email, passwordEncoder.encode("secret123"), rol);
		usuarios.guardar(usuario);
		String body = mvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return new Sesion(usuario.id(), json.readTree(body).get("accessToken").asText());
	}
}
