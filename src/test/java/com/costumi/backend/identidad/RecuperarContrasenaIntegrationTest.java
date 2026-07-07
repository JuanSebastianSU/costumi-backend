package com.costumi.backend.identidad;

import com.costumi.backend.TestcontainersConfiguration;
import com.costumi.backend.identidad.aplicacion.EnviadorDeEmail;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recuperación de contraseña (RF-1.1): olvide → token por email → restablecer. El envío de email se
 * captura con un doble de test (en prod es el adaptador SMTP gateado). No se revela si el correo existe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, RecuperarContrasenaIntegrationTest.EmailCaptor.class })
class RecuperarContrasenaIntegrationTest {

	/** Doble de test que captura el cuerpo del email (para extraer el token) en vez de enviarlo. */
	@TestConfiguration
	static class EmailCaptor {
		static final AtomicReference<String> ultimoCuerpo = new AtomicReference<>();

		@Bean
		@Primary
		EnviadorDeEmail enviadorCaptor() {
			return (destinatario, asunto, cuerpo) -> ultimoCuerpo.set(cuerpo);
		}
	}

	@Autowired
	MockMvc mvc;

	@Autowired
	ObjectMapper json;

	private void registrar(String correo, String password) throws Exception {
		mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk());
	}

	private static String extraerToken(String cuerpo) {
		Matcher m = Pattern.compile("es:\\R(\\S+)").matcher(cuerpo);
		assertThat(m.find()).as("el email debe contener el token").isTrue();
		return m.group(1);
	}

	@Test
	void flujo_completo_olvide_y_restablecer_cambia_la_contrasena() throws Exception {
		String correo = "recupera-" + UUID.randomUUID() + "@correo.com";
		registrar(correo, "claveVieja1");
		EmailCaptor.ultimoCuerpo.set(null);

		mvc.perform(post("/api/v1/auth/olvide").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\"}"))
				.andExpect(status().isNoContent());

		String token = extraerToken(EmailCaptor.ultimoCuerpo.get());

		mvc.perform(post("/api/v1/auth/restablecer").contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"" + token + "\",\"password\":\"claveNueva9\"}"))
				.andExpect(status().isNoContent());

		// La nueva contraseña funciona…
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"password\":\"claveNueva9\"}"))
				.andExpect(status().isOk());
		// …y la vieja ya no.
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"" + correo + "\",\"password\":\"claveVieja1\"}"))
				.andExpect(status().isUnauthorized());

		// El token es de un solo uso: no se puede reusar.
		mvc.perform(post("/api/v1/auth/restablecer").contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"" + token + "\",\"password\":\"otraClave99\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void olvide_con_correo_inexistente_igual_devuelve_204_sin_revelar() throws Exception {
		EmailCaptor.ultimoCuerpo.set(null);
		mvc.perform(post("/api/v1/auth/olvide").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"noexiste-" + UUID.randomUUID() + "@correo.com\"}"))
				.andExpect(status().isNoContent());
		assertThat(EmailCaptor.ultimoCuerpo.get()).as("no se envía email si el correo no existe").isNull();
	}

	@Test
	void restablecer_con_token_invalido_devuelve_400() throws Exception {
		mvc.perform(post("/api/v1/auth/restablecer").contentType(MediaType.APPLICATION_JSON)
						.content("{\"token\":\"token-que-no-existe\",\"password\":\"claveNueva9\"}"))
				.andExpect(status().isBadRequest());
	}
}
