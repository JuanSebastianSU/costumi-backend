package com.costumi.backend.pagos.adaptadores.salida;

import com.costumi.backend.pagos.dominio.PasarelaDePago;
import com.costumi.backend.pagos.dominio.PasarelaNoConfigurada;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Pasarela de pago vía MercadoPago (RF-6.11), <b>gateada</b>: sin access token configurado,
 * {@link #configurada()} es false y {@link #crearCheckout} lanza {@link PasarelaNoConfigurada} (→ 503).
 * Con credencial, crea una preferencia de checkout y devuelve su init_point.
 */
@Component
class PasarelaMercadoPago implements PasarelaDePago {

	private final String accessToken;

	// TODO(credenciales): COSTUMI_MP_ACCESS_TOKEN (MercadoPago). Alternativa: STRIPE_SECRET_KEY (otro adaptador).
	PasarelaMercadoPago(@Value("${costumi.pasarela.mp.access-token:}") String accessToken) {
		this.accessToken = accessToken;
	}

	@Override
	public boolean configurada() {
		return accessToken != null && !accessToken.isBlank();
	}

	@Override
	@SuppressWarnings("unchecked")
	public ResultadoCheckout crearCheckout(BigDecimal monto, String moneda, String referencia, String descripcion) {
		if (!configurada()) {
			throw new PasarelaNoConfigurada();
		}
		Map<String, Object> item = Map.of("title", descripcion, "quantity", 1,
				"currency_id", moneda, "unit_price", monto);
		Map<String, Object> body = Map.of("items", java.util.List.of(item), "external_reference", referencia);
		Map<String, Object> respuesta = RestClient.create().post()
				.uri("https://api.mercadopago.com/checkout/preferences")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(Map.class);
		String url = respuesta == null ? null : (String) respuesta.getOrDefault("init_point", null);
		String id = respuesta == null ? null : String.valueOf(respuesta.getOrDefault("id", ""));
		return new ResultadoCheckout(url, id);
	}
}
