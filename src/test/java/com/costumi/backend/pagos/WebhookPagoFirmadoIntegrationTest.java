package com.costumi.backend.pagos;

import com.costumi.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Firma del webhook de pagos (SEC-5), con el secreto configurado: una firma válida pasa; una inválida o
 * ausente se rechaza (401). Prueba el corazón del fix: sin la firma correcta no se puede confirmar un pago.
 */
@SpringBootTest(properties = "costumi.pasarela.mp.webhook-secret=" + WebhookPagoFirmadoIntegrationTest.SECRETO)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WebhookPagoFirmadoIntegrationTest {

	static final String SECRETO = "secreto-de-prueba-webhook";

	@Autowired
	MockMvc mvc;

	private static String cuerpo(UUID intentoId, String idPagoExterno) {
		return "{\"intentoId\":\"" + intentoId + "\",\"idPagoExterno\":\"" + idPagoExterno + "\"}";
	}

	private static String firma(UUID intentoId, String idPagoExterno) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		byte[] crudo = mac.doFinal((intentoId + ":" + idPagoExterno).getBytes(StandardCharsets.UTF_8));
		StringBuilder hex = new StringBuilder(crudo.length * 2);
		for (byte b : crudo) {
			hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
		}
		return hex.toString();
	}

	@Test
	void firma_valida_es_aceptada() throws Exception {
		UUID intento = UUID.randomUUID();
		String idPago = "mp-" + UUID.randomUUID();
		// Firma correcta: pasa el gate. El intento no existe -> confirmación idempotente no-op -> 204.
		mvc.perform(post("/api/v1/pagos/webhook").header("X-Signature", firma(intento, idPago))
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo(intento, idPago)))
				.andExpect(status().isNoContent());
	}

	@Test
	void firma_invalida_devuelve_401() throws Exception {
		UUID intento = UUID.randomUUID();
		mvc.perform(post("/api/v1/pagos/webhook").header("X-Signature", "0000firmafalsa0000")
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo(intento, "mp-1")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void firma_de_otro_contenido_devuelve_401() throws Exception {
		// Firma calculada para OTRO intento: no coincide con el contenido enviado -> 401 (anti-manipulación).
		UUID intento = UUID.randomUUID();
		String firmaDeOtro = firma(UUID.randomUUID(), "mp-1");
		mvc.perform(post("/api/v1/pagos/webhook").header("X-Signature", firmaDeOtro)
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo(intento, "mp-1")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void sin_firma_devuelve_401() throws Exception {
		UUID intento = UUID.randomUUID();
		mvc.perform(post("/api/v1/pagos/webhook")
						.contentType(MediaType.APPLICATION_JSON).content(cuerpo(intento, "mp-1")))
				.andExpect(status().isUnauthorized());
	}
}
