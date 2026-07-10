package com.costumi.backend.pagos.adaptadores.entrada;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Verifica la firma HMAC del webhook de pagos (SEC-5): sin esto, cualquiera que conozca un {@code intentoId}
 * podía marcar un pago como CONFIRMADO sin pagar (fraude). El emisor legítimo firma el contenido con un
 * secreto compartido; aquí se recalcula y se compara en tiempo constante.
 *
 * <p><b>Fail-closed:</b> si no hay secreto configurado, se rechaza el webhook (503) — no se aceptan
 * confirmaciones sin firmar.
 *
 * <p><i>Alcance:</i> firma HMAC-SHA256 sobre un contenido canónico ({@code intentoId:idPagoExterno}). Cuando se
 * integre MercadoPago real, se debe adaptar a su esquema {@code x-signature} (plantilla con {@code data.id},
 * {@code request-id} y {@code ts}) y sumar la confirmación consultando el estado del pago al proveedor (P-3).
 */
@Component
class VerificadorDeFirmaDeWebhook {

	private static final String ALGORITMO = "HmacSHA256";

	private final String secreto;

	VerificadorDeFirmaDeWebhook(@Value("${costumi.pasarela.mp.webhook-secret:}") String secreto) {
		this.secreto = secreto;
	}

	/** Exige que el webhook traiga una firma válida del {@code contenido}; si no, corta (503/401). */
	void exigirFirmaValida(String contenido, String firmaRecibida) {
		if (secreto == null || secreto.isBlank()) {
			throw new FirmaDeWebhookNoConfigurada();
		}
		if (firmaRecibida == null || firmaRecibida.isBlank() || !coincide(contenido, firmaRecibida.trim())) {
			throw new FirmaDeWebhookInvalida();
		}
	}

	private boolean coincide(String contenido, String firmaRecibida) {
		byte[] esperada = hmacHex(contenido).getBytes(StandardCharsets.UTF_8);
		byte[] recibida = firmaRecibida.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(esperada, recibida); // comparación en tiempo constante
	}

	private String hmacHex(String contenido) {
		try {
			Mac mac = Mac.getInstance(ALGORITMO);
			mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), ALGORITMO));
			byte[] crudo = mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(crudo.length * 2);
			for (byte b : crudo) {
				hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new IllegalStateException("No se pudo calcular la firma del webhook", e);
		}
	}
}
