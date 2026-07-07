package com.costumi.backend.pagos.adaptadores.salida;

import com.costumi.backend.pagos.dominio.PasarelaNoConfigurada;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** La pasarela está gateada: sin access token no está configurada y crear checkout da un 503 claro. */
class PasarelaMercadoPagoTest {

	@Test
	void sin_access_token_no_esta_configurada_y_crear_checkout_lanza_no_configurada() {
		PasarelaMercadoPago pasarela = new PasarelaMercadoPago("");
		assertThat(pasarela.configurada()).isFalse();
		assertThatThrownBy(() -> pasarela.crearCheckout(BigDecimal.TEN, "ARS", "ref", "desc"))
				.isInstanceOf(PasarelaNoConfigurada.class);
	}

	@Test
	void con_access_token_esta_configurada() {
		assertThat(new PasarelaMercadoPago("APP_USR-xxxx").configurada()).isTrue();
	}
}
