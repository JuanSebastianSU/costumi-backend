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
final class AuthTestHelper {

	private AuthTestHelper() {
	}

	static String token(MockMvc mvc, ObjectMapper json, UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
			UUID empresaId, Rol rol) throws Exception {
		String email = "u-" + UUID.randomUUID() + "@costumi.test";
		usuarios.guardar(Usuario.crear(empresaId, email, passwordEncoder.encode("secret123"), rol));
		String body = mvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return json.readTree(body).get("accessToken").asText();
	}
}
