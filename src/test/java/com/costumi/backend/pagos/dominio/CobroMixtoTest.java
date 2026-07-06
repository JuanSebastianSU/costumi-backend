package com.costumi.backend.pagos.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Dominio del cobro mixto (RF-6.7): total, parte en efectivo y cálculo del vuelto. */
class CobroMixtoTest {

	private PorcionDePago efectivo(String monto) {
		return new PorcionDePago(MetodoPago.EFECTIVO, new BigDecimal(monto), null);
	}

	private PorcionDePago tarjeta(String monto) {
		return new PorcionDePago(MetodoPago.TARJETA, new BigDecimal(monto), "AUTH-123");
	}

	@Test
	void el_total_es_la_suma_de_las_porciones() {
		CobroMixto cobro = new CobroMixto(List.of(efectivo("60.00"), tarjeta("40.00")), null);

		assertThat(cobro.total()).isEqualByComparingTo("100.00");
		assertThat(cobro.efectivoEsperado()).isEqualByComparingTo("60.00");
	}

	@Test
	void el_vuelto_es_el_efectivo_recibido_menos_la_parte_en_efectivo() {
		CobroMixto cobro = new CobroMixto(List.of(efectivo("60.00"), tarjeta("40.00")), new BigDecimal("100.00"));

		// Recibió 100 en efectivo pero la parte en efectivo era 60 -> vuelto 40.
		assertThat(cobro.vuelto()).isEqualByComparingTo("40.00");
	}

	@Test
	void sin_efectivo_recibido_el_vuelto_es_cero() {
		CobroMixto cobro = new CobroMixto(List.of(tarjeta("40.00")), null);

		assertThat(cobro.vuelto()).isEqualByComparingTo("0");
	}

	@Test
	void el_efectivo_recibido_debe_cubrir_la_parte_en_efectivo() {
		assertThatThrownBy(() -> new CobroMixto(List.of(efectivo("60.00")), new BigDecimal("50.00")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void un_cobro_necesita_al_menos_una_porcion() {
		assertThatThrownBy(() -> new CobroMixto(List.of(), null)).isInstanceOf(IllegalArgumentException.class);
	}
}
